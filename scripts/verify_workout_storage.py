"""Check generated Room auto-migration SQL with SQLite. Run after compileDebugKotlin.

Uses only Python's standard library and an in-memory database, never application data.
This complements JVM tests; it does not replace an Android installation smoke test.
Update MIGRATION_FROM / MIGRATION_TO when the schema version bumps so the script
keeps verifying the latest AutoMigration_<MIGRATION_FROM>_<MIGRATION_TO>_Impl.
"""

import json
import re
from pathlib import Path
import sqlite3
import unittest


ROOT = Path(__file__).resolve().parents[1]
SCHEMAS = ROOT / "app/schemas/dev.bugiel.kiroku.data.local.KirokuDatabase"
MIGRATION_FROM = 5
MIGRATION_TO = 6
GENERATED = (
    ROOT
    / "app/build/generated/ksp/debug/kotlin/dev/bugiel/kiroku/data/local"
    / f"KirokuDatabase_AutoMigration_{MIGRATION_FROM}_{MIGRATION_TO}_Impl.kt"
)


class WorkoutStorageTest(unittest.TestCase):
    def setUp(self):
        self.old = json.loads((SCHEMAS / f"{MIGRATION_FROM}.json").read_text(encoding="utf-8"))["database"]
        self.new = json.loads((SCHEMAS / f"{MIGRATION_TO}.json").read_text(encoding="utf-8"))["database"]
        self.db = sqlite3.connect(":memory:")
        self.addCleanup(self.db.close)
        self.db.execute("PRAGMA foreign_keys = ON")
        for entity in self.old["entities"]:
            table = entity["tableName"]
            self.db.execute(entity["createSql"].replace("${TABLE_NAME}", table))
            for index in entity.get("indices", []):
                self.db.execute(index["createSql"].replace("${TABLE_NAME}", table))
            columns = [field["columnName"] for field in entity["fields"]]
            values = [1 if field["affinity"] == "INTEGER" else "retained-" + field["columnName"] for field in entity["fields"]]
            names = ", ".join(f'"{column}"' for column in columns)
            self.db.execute(f'INSERT INTO "{table}" ({names}) VALUES ({", ".join("?" for _ in values)})', values)
        self.before = self.existing_rows()
        self.old_definitions = self.db.execute("SELECT name, sql FROM sqlite_master WHERE sql IS NOT NULL ORDER BY name").fetchall()
        self.db.commit()
        source = GENERATED.read_text(encoding="utf-8")
        self.statements = [json.loads(value) for value in re.findall(r'connection\.execSQL\(("(?:[^"\\]|\\.)*")\)', source)]
        self.assertEqual(len(self.statements), 10)
        with self.db:
            for sql in self.statements:
                self.assertTrue(sql.startswith(("CREATE TABLE", "CREATE INDEX", "CREATE UNIQUE INDEX")))
                self.db.execute(sql)

    def existing_rows(self):
        return {entity["tableName"]: self.db.execute(f'SELECT * FROM "{entity["tableName"]}"').fetchall() for entity in self.old["entities"]}

    def populate_workout(self):
        with self.db:
            self.db.execute("INSERT INTO workout_plans VALUES (1, 'Push', 123)")
            self.db.execute("INSERT INTO workout_exercises VALUES (1, 1, 'Bench', 0, 0)")
            self.db.execute("INSERT INTO workout_sessions VALUES (1, 1, 20000, 123)")
            self.db.execute("INSERT INTO workout_sessions VALUES (2, 1, 20001, 456)")
            self.db.execute("INSERT INTO workout_session_exercises VALUES (1, 1, 1, 'Bench', 0)")
            self.db.execute("INSERT INTO workout_session_exercises VALUES (2, 2, 1, 'Bench', 0)")
            self.db.execute("INSERT INTO workout_sets VALUES (1, 1, 0, 82.5, 8)")
            self.db.execute("INSERT INTO workout_sets VALUES (2, 2, 0, 85, 6)")

    def test_migration_preserves_every_existing_table_index_and_row(self):
        self.assertEqual(self.existing_rows(), self.before)
        current = dict(self.db.execute("SELECT name, sql FROM sqlite_master WHERE sql IS NOT NULL"))
        for name, definition in self.old_definitions:
            self.assertEqual(current[name], definition)
        old_entities = {e["tableName"]: e for e in self.old["entities"]}
        new_entities = {e["tableName"]: e for e in self.new["entities"]}
        for table, entity in old_entities.items():
            self.assertEqual(new_entities[table], entity)
        self.assertEqual(len(new_entities) - len(old_entities), 5)
        self.assertEqual(self.db.execute("PRAGMA integrity_check").fetchone(), ("ok",))
        self.assertEqual(self.db.execute("PRAGMA foreign_key_check").fetchall(), [])

    def test_session_uniqueness_and_foreign_keys(self):
        self.populate_workout()
        with self.assertRaises(sqlite3.IntegrityError), self.db:
            self.db.execute("INSERT INTO workout_sessions VALUES (3, 1, 20000, 789)")
        with self.assertRaises(sqlite3.IntegrityError), self.db:
            self.db.execute("INSERT INTO workout_sets VALUES (3, 999, 0, 70, 8)")
        self.assertEqual(self.db.execute("SELECT count(*) FROM workout_sessions").fetchone(), (2,))

    def test_failed_set_replacement_rolls_back_the_previous_sets(self):
        self.populate_workout()
        with self.assertRaises(sqlite3.IntegrityError), self.db:
            self.db.execute("DELETE FROM workout_sets WHERE sessionExerciseId = 1")
            self.db.execute("INSERT INTO workout_sets VALUES (3, 1, 0, 60, 10)")
            self.db.execute("INSERT INTO workout_sets VALUES (4, 1, 0, 70, 10)")
        self.assertEqual(self.db.execute("SELECT weightKg, reps FROM workout_sets WHERE sessionExerciseId = 1").fetchall(), [(82.5, 8)])

    def test_archiving_and_renaming_leave_historical_entries_intact(self):
        self.populate_workout()
        with self.db:
            self.db.execute("UPDATE workout_exercises SET name = 'Incline bench', isArchived = 1 WHERE id = 1")
        self.assertEqual(self.db.execute("SELECT name FROM workout_session_exercises").fetchall(), [("Bench",), ("Bench",)])
        self.assertEqual(self.db.execute("SELECT max(weightKg) FROM workout_sets").fetchone(), (85.0,))

    def test_deleting_a_day_removes_only_its_entries_and_sets(self):
        self.populate_workout()
        with self.db:
            self.db.execute("DELETE FROM workout_sessions WHERE id = 2")
        self.assertEqual(self.db.execute("SELECT id FROM workout_sets").fetchall(), [(1,)])
        self.assertEqual(self.db.execute("SELECT id FROM workout_exercises").fetchall(), [(1,)])
        self.assertEqual(self.existing_rows(), self.before)

    def test_deleting_a_plan_does_not_touch_other_plans_or_existing_app_data(self):
        self.populate_workout()
        with self.db:
            self.db.execute("INSERT INTO workout_plans VALUES (2, 'Pull', 999)")
            self.db.execute("DELETE FROM workout_plans WHERE id = 1")
        self.assertEqual(self.db.execute("SELECT name FROM workout_plans").fetchall(), [("Pull",)])
        for table in ["workout_exercises", "workout_sessions", "workout_session_exercises", "workout_sets"]:
            self.assertEqual(self.db.execute(f'SELECT count(*) FROM "{table}"').fetchone(), (0,))
        self.assertEqual(self.existing_rows(), self.before)
        self.assertEqual(self.db.execute("PRAGMA foreign_key_check").fetchall(), [])


if __name__ == "__main__":
    unittest.main(verbosity=2)
