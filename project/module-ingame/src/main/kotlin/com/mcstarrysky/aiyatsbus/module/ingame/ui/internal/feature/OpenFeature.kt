/*
 * This file is part of ParrotX, licensed under the MIT License.
 *
 *  Copyright (c) 2020 Legoshi
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */
package com.mcstarrysky.aiyatsbus.module.ingame.ui.internal.feature

import com.mcstarrysky.aiyatsbus.module.ingame.ui.internal.MenuFeature
import com.mcstarrysky.aiyatsbus.module.ingame.ui.internal.data.ActionContext
import com.mcstarrysky.aiyatsbus.module.ingame.ui.internal.function.value
import com.mcstarrysky.aiyatsbus.module.ingame.ui.internal.registry.MenuOpeners

@Suppress("unused")
object OpenFeature : MenuFeature() {

    override val name: String = "Open"

    override fun handle(context: ActionContext) {
        val keyword = context.extra.value<String>("keyword")
        requireNotNull(MenuOpeners[keyword]) { "未知的 MenuOpener: $keyword" }(context)
    }

}