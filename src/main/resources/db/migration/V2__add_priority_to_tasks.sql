-- Priorita ulohy: 0 = ziadna, 5 = najvyssia.
-- DEFAULT 0 je nutny, lebo tabulka uz obsahuje riadky a NOT NULL stlpec
-- bez predvolenej hodnoty by sa na nich nedal pridat.
ALTER TABLE tasks ADD COLUMN priority INTEGER NOT NULL DEFAULT 0;
