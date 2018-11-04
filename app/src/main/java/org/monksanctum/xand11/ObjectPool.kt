// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.monksanctum.xand11

import org.monksanctum.xand11.ObjectPool.Recycleable

import java.util.ArrayList

abstract class ObjectPool<T : Recycleable, in V> {

    private var mAvailable = ArrayList<T>()

    fun obtain(vararg arg: V): T {
        synchronized(mAvailable) {
            for (i in mAvailable.indices) {
                if (validate(mAvailable[i], *arg)) {
                    return mAvailable.removeAt(i)
                }
            }
        }
        val t = create(*arg)!!
        t.mPool = this
        return t
    }

    private fun recycle(recycleable: Any) {
        synchronized(mAvailable) {
            mAvailable.add(recycleable as T)
        }
    }

    protected open fun validate(inst: T, vararg arg: V): Boolean {
        return true
    }

    protected abstract fun create(vararg arg: V): T?

    open class Recycleable : AutoCloseable {
        var mPool: ObjectPool<*, *>? = null

        override fun close() {
            recycle()
        }

        fun recycle() {
            mPool!!.recycle(this)
        }
    }
}
