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
package com.mcstarrysky.aiyatsbus.module.ingame.ui.internal.feature.util

import com.mcstarrysky.aiyatsbus.module.ingame.ui.internal.data.ActionContext

interface VariableProvider {

    val name: String

    fun produce(context: ActionContext): String

    operator fun invoke(context: ActionContext): String = produce(context)

}

@Suppress("unused")
class VariableProviderBuilder(name: String? = null, builder: VariableProviderBuilder.() -> Unit) : VariableProvider {

    override var name: String = name ?: ""
        internal set

    private var producer: (ActionContext) -> String = { "%${name}%" }

    init {
        builder()
    }

    fun onProduce(block: (ActionContext) -> String) {
        producer = block
    }

    override fun produce(context: ActionContext): String = producer(context)

}