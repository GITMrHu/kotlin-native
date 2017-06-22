/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package kotlin.text.regex

/**
 * Optimized greedy quantifier node ('*') for the case where there is no intersection with
 * next node and normal quantifiers could be treated as greedy and possessive.
 */
internal class UnifiedQuantifierSet(quant: LeafQuantifierSet) : LeafQuantifierSet(Quantifier.starQuantifier, quant.leaf, quant.next, quant.type) {

    override fun matches(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        var index = startIndex
        while (index + leaf.charCount <= testString.length && leaf.accepts(index, testString) > 0) {
            index += leaf.charCount
        }

        return next.matches(index, testString, matchResult)
    }

    override fun find(startIndex: Int, testString: CharSequence, matchResult: MatchResultImpl): Int {
        var startSearch = next.find(startIndex, testString, matchResult)
        if (startSearch < 0)
            return -1

        var result = startSearch
        var index = startSearch - leaf.charCount
        while (index >= startIndex && leaf.accepts(index, testString) > 0) {
            result = index
            index -= leaf.charCount
        }
        return result
    }

    init {
        innerSet.next = this
    }
}