/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.shade;

import com.android.systemui.shade.data.repository.ShadeRepository;
import com.android.systemui.shade.data.repository.ShadeRepositoryImpl;

import dagger.Binds;
import dagger.Module;

/** Provides Shade-related events and information. */
@Module
public abstract class ShadeEventsModule {
    @Binds
    abstract ShadeStateEvents bindShadeEvents(ShadeExpansionStateManager impl);

    @Binds abstract ShadeRepository shadeRepository(ShadeRepositoryImpl impl);
}
