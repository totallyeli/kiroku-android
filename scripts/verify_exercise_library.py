"""Execute the real 6→7 Room migration against populated SQLite databases.

Run after compileDebugKotlin (schema export and WorkoutMigrationKt.class required).
Only standard-library, in-memory SQLite is used. No application data is accessed.
The older 5→6 auto-migration remains covered by verify_workout_storage.py.
"""

import json
from pathlib import Path
import re
import sqlite3
import textwrap
import unittest


ROOT = Path(__file__).resolve().parents[1]
SCHEMAS = ROOT / "app/schemas/dev.bugiel.kiroku.data.local.KirokuDatabase"
MIGRATION = ROOT / "app/src/main/java/dev/bugiel/kiroku/data/local/WorkoutMigration.kt"
DAO = ROOT / "app/src/main/java/dev/bugiel/kiroku/data/local/dao/WorkoutDao.kt"
PACKAGE = "dev/bugiel/kiroku/data/local/WorkoutMigrationKt.class"


def load_schema(version):
    return json.loads((SCHEMAS / f"{version}.json").read_text(encoding="utf-8"))["database"]


def create_schema(db, schema):
    for entity in schema["entities"]:
        table = entity["tableName"]
        db.execute(entity["createSql"].replace("${TABLE_NAME}", table))
        for index in entity.get("indices", []):
            db.execute(index["createSql"].replace("${TABLE_NAME}", table))


def migration_statements():
    """Read literal execSQL calls, refusing to silently skip dynamic SQL."""
    source = MIGRATION.read_text(encoding="utf-8")
    if not re.search(r"\bval\s+MIGRATION_6_7\b", source):
        raise AssertionError("WorkoutMigration.kt must expose top-level MIGRATION_6_7")
    source = source[source.index("val MIGRATION_6_7"):]
    pattern = r'\b(?:connection|db)\.execSQL\(\s*("""[\s\S]*?"""(?:\.trimIndent\(\))?|"(?:[^"\\]|\\.)*")\s*\)'
    literals = re.findall(pattern, source)
    calls = re.findall(r"\b(?:connection|db)\.execSQL\(", source)
    if not literals or len(literals) != len(calls):
        raise AssertionError("Every migration execSQL must be a readable literal; no SQL may be skipped")
    statements = []
    for literal in literals:
        if literal.startswith('"""'):
            sql = literal[3:literal.rindex('"""')]
            if literal.endswith(".trimIndent()"):
                sql = textwrap.dedent(sql).strip("\n")
        else:
            sql = json.loads(literal)
        # SQLite placeholders are allowed, Kotlin interpolation is not.
        if "${" in sql:
            raise AssertionError("Migration SQL interpolation is not supported")
        statements.append(sql)
    return statements


def schema_fingerprint(db):
    """Compare actual columns, defaults, PK order, indexes and FK actions."""
    tables = [row[0] for row in db.execute(
        "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name"
    )]
    result = {}
    for table in tables:
        indexes = []
        for row in db.execute(f'PRAGMA index_list("{table}")'):
            # autoindex names reflect SQLite implementation, not Room's contract.
            name = row[1]
            indexes.append((None if name.startswith("sqlite_autoindex_") else name,
                            row[2], row[3], row[4],
                            tuple(db.execute(f'PRAGMA index_xinfo("{name}")'))))
        result[table] = (
            tuple(db.execute(f'PRAGMA table_info("{table}")')),
            tuple(sorted(tuple(row[1:]) for row in db.execute(f'PRAGMA foreign_key_list("{table}")'))),
            tuple(sorted(indexes, key=repr)),
        )
    return result


class ExerciseLibraryStorageTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        required = [MIGRATION, DAO, SCHEMAS / "6.json", SCHEMAS / "7.json"]
        for path in required:
            if not path.is_file():
                raise AssertionError(f"Required migration/schema file absent: {path}; run compileDebugKotlin")
        candidates = [ROOT / "app/build/tmp/kotlin-classes/debug" / PACKAGE,
                      ROOT / "app/build/intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes" / PACKAGE]
        compiled = next((path for path in candidates if path.is_file()), None)
        if compiled is None:
            raise AssertionError("WorkoutMigrationKt.class absent; migration has not been compiled. Run compileDebugKotlin")
        if compiled.stat().st_mtime_ns < MIGRATION.stat().st_mtime_ns:
            raise AssertionError("WorkoutMigrationKt.class is stale; recompile the migration")
        cls.old = load_schema(6)
        cls.new = load_schema(7)
        cls.statements = migration_statements()

    def setUp(self):
        self.db = sqlite3.connect(":memory:")
        self.addCleanup(self.db.close)
        self.db.execute("PRAGMA foreign_keys = ON")
        create_schema(self.db, self.old)
        self.seed_v6()
        self.before = self.rows()
        self.sequences_before = self.db.execute("SELECT name, seq FROM sqlite_sequence ORDER BY name").fetchall()
        self.db.commit()
        # Room wraps the entire migration, including DDL, in one transaction.
        self.db.execute("BEGIN")
        with self.db:
            for sql in self.statements:
                self.db.execute(sql)
        self.assertEqual(self.db.execute("PRAGMA foreign_keys").fetchone(), (1,))
        self.assertEqual(self.db.execute("PRAGMA foreign_key_check").fetchall(), [])

    def seed_v6(self):
        # Populate EVERY non-workout table, preserving nulls, Unicode and exact values.
        for entity in self.old["entities"]:
            table = entity["tableName"]
            if table.startswith("workout_"):
                continue
            columns = [field["columnName"] for field in entity["fields"]]
            for number in (1, 2):
                values = []
                for field in entity["fields"]:
                    if not field.get("notNull") and number == 2:
                        value = None
                    elif field["affinity"] == "INTEGER":
                        value = number
                    elif field["affinity"] == "REAL":
                        value = number + 0.25
                    else:
                        value = f"retained-{number}-{field['columnName']}-Übung"
                    values.append(value)
                names = ", ".join(f'"{column}"' for column in columns)
                self.db.execute(f'INSERT INTO "{table}" ({names}) VALUES ({", ".join("?" for _ in values)})', values)
        self.db.executemany("INSERT INTO workout_plans VALUES (?, ?, ?)",
                            [(11, "Push", 1000), (22, "Full body", 2000)])
        self.db.executemany("INSERT INTO workout_exercises VALUES (?, ?, ?, ?, ?)", [
            (101, 11, "Bench", 4, 0), (102, 11, "Archived fly", 9, 1),
            (103, 22, "Bench", 0, 0), (104, 22, "Row Übung", 3, 0),
        ])
        self.db.executemany("INSERT INTO workout_sessions VALUES (?, ?, ?, ?)", [
            (201, 11, 20000, 123456), (202, 22, 20001, 234567), (203, 11, 20002, 345678),
        ])
        self.db.executemany("INSERT INTO workout_session_exercises VALUES (?, ?, ?, ?, ?)", [
            (301, 201, 101, "Original bench snapshot", 7),
            (302, 201, 102, "Original fly snapshot", 11),
            (303, 202, 103, "Other plan bench snapshot", 0),
        ])
        self.db.executemany("INSERT INTO workout_sets VALUES (?, ?, ?, ?, ?)", [
            (401, 301, 0, 82.5, 8), (402, 301, 1, 85.0, 6),
            (403, 302, 0, 0.0, 12), (404, 303, 4, 102.75, 3),
        ])
        # Deleted IDs still form part of AUTOINCREMENT identity guarantees.
        self.db.execute("INSERT INTO workout_exercises VALUES (9101, 11, 'Deleted', 99, 0)")
        self.db.execute("INSERT INTO workout_session_exercises VALUES (9301, 201, 9101, 'Deleted', 99)")
        self.db.execute("INSERT INTO workout_sets VALUES (9401, 9301, 0, 1, 1)")
        self.db.execute("DELETE FROM workout_exercises WHERE id = 9101")

    def rows(self):
        return {entity["tableName"]: self.db.execute(
            f'SELECT * FROM "{entity["tableName"]}" ORDER BY rowid'
        ).fetchall() for entity in self.old["entities"]}

    def test_empty_v6_database_also_migrates_without_temporary_table_leaks(self):
        empty = sqlite3.connect(":memory:")
        self.addCleanup(empty.close)
        empty.execute("PRAGMA foreign_keys = ON")
        create_schema(empty, self.old)
        empty.execute("BEGIN")
        with empty:
            for sql in self.statements:
                empty.execute(sql)
        self.assertEqual(schema_fingerprint(empty), schema_fingerprint(self.db))
        self.assertEqual(empty.execute("SELECT name FROM sqlite_temp_master WHERE type = 'table'").fetchall(), [])
        self.assertEqual(empty.execute("PRAGMA foreign_key_check").fetchall(), [])
        with empty:
            exercise = empty.execute("INSERT INTO workout_exercises (name, isArchived) VALUES ('First', 0)").lastrowid
        self.assertEqual(exercise, 1)

    def test_migration_matches_exported_v7_columns_indexes_and_foreign_keys(self):
        expected = sqlite3.connect(":memory:")
        self.addCleanup(expected.close)
        create_schema(expected, self.new)
        self.assertEqual(schema_fingerprint(self.db), schema_fingerprint(expected))
        self.assertEqual(self.new["version"], 7)
        self.assertEqual(self.db.execute("PRAGMA integrity_check").fetchone(), ("ok",))

    def test_every_non_workout_row_and_workout_snapshot_id_and_value_survives(self):
        after = self.rows()
        for table, rows in self.before.items():
            if table != "workout_exercises":
                self.assertEqual(after[table], rows, table)
        self.assertEqual(self.db.execute("SELECT id, name, isArchived FROM workout_exercises ORDER BY id").fetchall(), [
            (101, "Bench", 0), (102, "Archived fly", 1), (103, "Bench", 0), (104, "Row Übung", 0),
        ])
        self.assertEqual(self.db.execute("SELECT name, seq FROM sqlite_sequence ORDER BY name").fetchall(), self.sequences_before)
        self.assertEqual(self.db.execute("SELECT * FROM workout_plan_exercises ORDER BY planId, position").fetchall(), [
            (11, 101, 4), (11, 102, 9), (22, 103, 0), (22, 104, 3),
        ])

    def test_same_named_old_exercises_are_not_merged_and_archived_assignments_survive(self):
        self.assertEqual(self.db.execute("SELECT id FROM workout_exercises WHERE name = 'Bench' ORDER BY id").fetchall(), [(101,), (103,)])
        self.assertEqual(self.db.execute("SELECT exerciseId FROM workout_plan_exercises WHERE planId = 11 ORDER BY position").fetchall(), [(101,), (102,)])
        self.assertEqual(self.db.execute("SELECT * FROM workout_session_exercises WHERE id = 302").fetchone(), self.before["workout_session_exercises"][1])

    def test_library_reuse_unassignment_and_deleting_plan_keep_other_history(self):
        with self.db:
            self.db.execute("INSERT INTO workout_plan_exercises VALUES (22, 101, 8)")
            self.db.execute("INSERT INTO workout_session_exercises VALUES (304, 202, 101, 'Shared bench', 8)")
            self.db.execute("INSERT INTO workout_sets VALUES (405, 304, 0, 110.5, 4)")
            self.db.execute("DELETE FROM workout_plan_exercises WHERE planId = 22 AND exerciseId = 101")
        self.assertEqual(self.db.execute("SELECT weightKg FROM workout_sets WHERE sessionExerciseId = 304").fetchall(), [(110.5,)])
        with self.db:
            self.db.execute("DELETE FROM workout_plans WHERE id = 11")
        self.assertEqual(self.db.execute("SELECT id FROM workout_exercises ORDER BY id").fetchall(), [(101,), (102,), (103,), (104,)])
        self.assertEqual(self.db.execute("SELECT id FROM workout_session_exercises ORDER BY id").fetchall(), [(303,), (304,)])
        self.assertEqual(self.db.execute("SELECT id FROM workout_sets ORDER BY id").fetchall(), [(404,), (405,)])
        for table, rows in self.before.items():
            if not table.startswith("workout_"):
                self.assertEqual(self.db.execute(f'SELECT * FROM "{table}" ORDER BY rowid').fetchall(), rows)
        self.assertEqual(self.db.execute("PRAGMA foreign_key_check").fetchall(), [])

    def test_assignment_and_entry_uniqueness_and_foreign_keys_are_enforced(self):
        invalid = [
            "INSERT INTO workout_plan_exercises VALUES (11, 101, 99)",
            "INSERT INTO workout_plan_exercises VALUES (999, 101, 0)",
            "INSERT INTO workout_plan_exercises VALUES (11, 999, 0)",
            "INSERT INTO workout_session_exercises VALUES (305, 201, 101, 'Duplicate', 99)",
            "INSERT INTO workout_session_exercises VALUES (305, 201, 999, 'Missing', 99)",
            "INSERT INTO workout_sets VALUES (405, 999, 0, 10, 1)",
            "INSERT INTO workout_sets VALUES (405, 301, 0, 10, 1)",
        ]
        for sql in invalid:
            with self.subTest(sql=sql), self.assertRaises(sqlite3.IntegrityError), self.db:
                self.db.execute(sql)
        self.assertEqual(self.db.execute("SELECT * FROM workout_sets ORDER BY id").fetchall(), self.before["workout_sets"])

    def test_actual_global_history_query_finds_logged_shared_identity_across_plans(self):
        source = DAO.read_text(encoding="utf-8")
        match = re.search(r'@Query\("""([\s\S]*?)"""\)\s*abstract fun observeExerciseHistory\(', source)
        self.assertIsNotNone(match, "Global history query must be readable from WorkoutDao.kt")
        sql = textwrap.dedent(match.group(1)).strip()
        with self.db:
            self.db.execute("INSERT INTO workout_session_exercises VALUES (304, 202, 101, 'Shared bench snapshot', 5)")
            self.db.execute("INSERT INTO workout_sets VALUES (405, 304, 0, 120.25, 2)")
            self.db.execute("INSERT INTO workout_session_exercises VALUES (305, 203, 101, 'Unlogged', 0)")
        cursor = self.db.execute(sql, {"exerciseId": 101})
        names = [column[0] for column in cursor.description]
        history = [dict(zip(names, row)) for row in cursor.fetchall()]
        self.assertEqual([row["id"] for row in history], [304, 301])
        self.assertEqual([row["planId"] for row in history], [22, 11])
        self.assertEqual([row["name"] for row in history], ["Shared bench snapshot", "Original bench snapshot"])
        self.assertEqual(self.db.execute(sql, {"exerciseId": 999}).fetchall(), [])

    def test_new_ids_follow_retained_ids_and_empty_workouts_are_valid(self):
        with self.db:
            exercise = self.db.execute("INSERT INTO workout_exercises (name, isArchived) VALUES ('New', 0)").lastrowid
            session = self.db.execute("INSERT INTO workout_sessions (planId, epochDay, createdAt) VALUES (22, 20003, 999)").lastrowid
        self.assertGreater(exercise, 9101)
        self.assertGreater(session, 203)
        self.assertEqual(self.db.execute("SELECT count(*) FROM workout_session_exercises WHERE sessionId = ?", (session,)).fetchone(), (0,))
        with self.db:
            entry = self.db.execute("INSERT INTO workout_session_exercises (sessionId, exerciseId, name, position) VALUES (?, ?, 'New snapshot', 0)", (session, exercise)).lastrowid
            saved_set = self.db.execute("INSERT INTO workout_sets (sessionExerciseId, position, weightKg, reps) VALUES (?, 0, 10, 5)", (entry,)).lastrowid
        self.assertGreater(entry, 9301)
        self.assertGreater(saved_set, 9401)


if __name__ == "__main__":
    unittest.main(verbosity=2)
