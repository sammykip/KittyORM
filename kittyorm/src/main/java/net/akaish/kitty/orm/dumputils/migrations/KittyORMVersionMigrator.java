
/*
 * ---
 *
 *  Copyright (c) 2018 Denis Bogomolov (akaish)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This file is a part of KittyORM project (KittyORM library), more information at
 * https://akaish.github.io/KittyORMPages/license/
 *
 * ---
 */

package net.akaish.kitty.orm.dumputils.migrations;

import android.content.Context;
import android.util.Log;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Abstract databaseClass versions migrator
 * Created by akaish on 05.03.18.
 * @author akaish (Denis Bogomolov)
 */
public abstract class KittyORMVersionMigrator {

    protected final LinkedList<KittyMigration> migrations = new LinkedList<>();

    protected final KittyMigrationFactory migrationFactory;

    protected final int oldVersion;
    protected final int currentVersion;
    protected final String schemaName;
    protected final String logTag;
    protected final boolean logOn;

    protected final Context context;

    protected final String AME_SEQUENCE_NOT_APPLICABLE = "Migration sequence generated by {0} for schema {1} not applicable (sequence doesn't have all steps for version {2} to version {3})!";

    public KittyORMVersionMigrator(int oldVersion, int currentVersion, Context ctx, String schemaName,
                                   String logTag, boolean logOn, Object[] factoryParameters, Object[] migrationsParameters) {
        this.logOn = logOn;
        this.logTag = logTag;
        this.oldVersion = oldVersion;
        this.schemaName = schemaName;
        this.currentVersion = currentVersion;
        this.context = ctx;
        setParameters(factoryParameters, migrationsParameters);
        migrationFactory = getMigrationFactory(factoryParameters);
        setMigrations(migrationsParameters);
        orderMigrationsList();
    }


    protected abstract void setParameters(Object[] factoryParameters, Object[] migrationsParameters);

    /**
     * Returns child depended migrations factory
     * @param <T>
     * @return
     */
    protected abstract <T extends KittyMigrationFactory> T getMigrationFactory(Object... parameters);

    /**
     * Fills {@link #migrations} with {@link #migrationFactory}
     */
    protected abstract void setMigrations(Object... parameters);

    /**
     * Orders migrations in {@link #migrations} list in ascending order by migration's minVersionLower field
     */
    protected void orderMigrationsList() {
        Collections.sort(migrations, new Comparator<KittyMigration>() {
            @Override
            public int compare(KittyMigration o1, KittyMigration o2) {
                if(o1.getMinVersionLower() < o2.getMinVersionLower())
                    return -1;
                else
                    return 1;
            }
        });
    }

    /**
     * Returns true if migration sequence can be applied step by step from old to current databaseClass versions
     * @return
     */
    public boolean isMigrationSequenceApplicable() {
        if(migrations == null) return false;
        if(migrations.size() == 0) return false;
        if(migrations.size() == 1) {
            return  migrations.get(0).getMinVersionLower() <= oldVersion;
        }
        Iterator<KittyMigration> migrationIterator = migrations.iterator();
        KittyMigration first = null;
        KittyMigration last = null;
        KittyMigration current = null;
        while (migrationIterator.hasNext()) {
            KittyMigration km = migrationIterator.next();
            if(km.getMinVersionLower() < oldVersion) continue; // just skip
            if(first == null) {
                first = km;
                last = first;
                continue;
            }
            current = last;
            last = km;
            if(current.getMaxVersionUpper() < last.getMinVersionLower())
                return false;
        }
        if(first == null) return false;
        if(last == null)
            return first.getMinVersionLower() <= oldVersion;
        return (first.getMinVersionLower() >= oldVersion) && (last.getMaxVersionUpper() <= currentVersion);
    }

    /**
     * Returns iterator for {@link #migrations} if migration sequence is applicable to current schema,
     * or null if not applicable. If you wish to get migrations for debug or other purposes, use
     * {@link #getMigrations()} instead.
     * @return
     */
    public Iterator<KittyMigration> getMigrationsIterator() {
        if(isMigrationSequenceApplicable())
            return migrations.iterator();
        else {
            if(logOn) {
                Log.e(logTag, MessageFormat.format(
                        AME_SEQUENCE_NOT_APPLICABLE,
                        getClass().getCanonicalName(),
                        schemaName,
                        Integer.toString(oldVersion),
                        Integer.toString(currentVersion)));
            }
            return null;
        }
    }

    /**
     * Returns list of sorted migrations. You better use {@link #getMigrationsIterator()} for
     * applying migration sequence cause this method won't check whether migration sequence applicable
     * or not.
     * @return
     */
    public LinkedList<KittyMigration> getMigrations() {
        return migrations;
    }
}
