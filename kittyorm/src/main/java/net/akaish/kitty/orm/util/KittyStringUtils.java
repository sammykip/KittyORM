
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
 * @author akaish
 */
public class KittyStringUtils {
    /**
     * Returns versions as a string imploded with usage of comma-whitespace separator
     * @param versions
     * @return
     */
    public static final String versionsArrayString(int[] versions) {
        String[] versionsStrings = new String[versions.length];
        int counter = 0;
        for(int version : versions) {
            versionsStrings[counter] = Integer.toString(version);
            counter++;
        }
        return KittyUtils.implode(versionsStrings, ", ");
    }
}
