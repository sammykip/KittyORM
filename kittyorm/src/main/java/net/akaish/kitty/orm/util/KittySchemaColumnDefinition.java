
/*
 * ---
 *
 *  Copyright (c) 2019 Denis Bogomolov (akaish)
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

package net.akaish.kitty.orm.util;

/**
 * Model for checking schema
 * Created by akaish on 1.6.19.
 * @author akaish (Denis Bogomolov)
 */
public class KittySchemaColumnDefinition {

    public enum PRAGMA_TYPES {
        text, integer, real, blob
    }

    final String name;
    final PRAGMA_TYPES type;
    final int notNull;
    final int pk;

    boolean checked = false;

    KittySchemaColumnDefinition(String name, PRAGMA_TYPES type, int notNull, int pk) {
        this.name = name;
        this.type = type;
        this.notNull = notNull;
        this.pk = pk;
    }

    public String getName() {
        return name;
    }

    public PRAGMA_TYPES getType() {
        return type;
    }

    public int getNotNull() {
        return notNull;
    }

    public int getPk() {
        return pk;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
