
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
 * This file is a part of KittyORM project (KittyORM Demo), more information at
 * https://akaish.github.io/KittyORMPages/license/
 *
 * ---
 */

package net.akaish.kittyormdemo.sqlite.migrations.migv1;

import android.content.Context;

import net.akaish.kitty.orm.annotations.KITTY_DATABASE;
import net.akaish.kitty.orm.KittyDatabase;
import net.akaish.kitty.orm.annotations.KITTY_DATABASE_REGISTRY;

/**
 * Created by akaish on 03.10.18.
 * @author akaish (Denis Bogomolov)
 */
@KITTY_DATABASE(
        isLoggingOn = true,
        isProductionOn = false,
        databaseName = "mig",
        databaseVersion = 1,
        logTag = MigrationDBv1.LTAG,
        domainPackageNames = {"net.akaish.kittyormdemo.sqlite.migrations.migv1"}
)
@KITTY_DATABASE_REGISTRY(
        domainModels = {net.akaish.kittyormdemo.sqlite.migrations.migv1.MigOneModel.class}
)
public class MigrationDBv1 extends KittyDatabase {

    public static final String LTAG = "MIGv1";

    /**
     * KittyORM main database class that represents bootstrap and holder for all related with database
     * components.
     *
     * @param ctx
     */
    public MigrationDBv1(Context ctx) {
        super(ctx);
    }
}
