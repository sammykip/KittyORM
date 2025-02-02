
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

package net.akaish.kittyormdemo.fabmenu;

import android.view.View;

/**
 * Created by akaish on 07.08.18.
 * @author akaish (Denis Bogomolov)
 */

public abstract class FabOnClickListenerFabMenuStateSpecific implements View.OnClickListener {

    protected boolean isFabMenuOpen;
    protected int snackbarResId;

    public FabOnClickListenerFabMenuStateSpecific() {
        snackbarResId = -1;
    }

    public FabOnClickListenerFabMenuStateSpecific(int snackbarRes) {
        snackbarResId = snackbarRes;
    }

    public void setFabMenuOpen(boolean isFabMenuOpen) {
        this.isFabMenuOpen = isFabMenuOpen;
    }
}
