/*
 * Copyright 2007 University of Waikato.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */

package maia.ml.learner.standard.hoeffdingtree

/*
 * Copyright 2007 University of Waikato.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */



/**
 * Multi choice option.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
class MultiChoiceOption(
    name: String?, cliChar: Char, purpose: String?,
    optionLabels: Array<String>, optionDescriptions: Array<String>,
    defaultOptionIndex: Int
) :
    AbstractOption(name, cliChar, purpose) {
    protected var optionLabels: Array<String>
    protected var optionDescriptions: Array<String>
    var defaultOptionIndex: Int
        protected set
    protected var chosenOptionIndex = 0
    override val defaultCLIString: String
        get() = optionLabels[defaultOptionIndex]
    override val valueAsCLIString: String
        get() = chosenLabel!!

    override fun setValueViaCLIString(s: String?) {
        try {
            chosenIndex = s!!.trim { it <= ' ' }.toInt()
        } catch (nfe: NumberFormatException) {
            chosenLabel = s
        }
    }

    /*fun getOptionLabels(): Array<String> {
        return optionLabels.clone()
    }*/

    /*fun getOptionDescriptions(): Array<String> {
        return optionDescriptions.clone()
    }*/

    var chosenLabel: String?
        get() = optionLabels[chosenOptionIndex]
        set(label) {
            var label = label
            label = label!!.trim { it <= ' ' }
            for (i in optionLabels.indices) {
                if (optionLabels[i] == label) {
                    chosenOptionIndex = i
                    return
                }
            }
            throw IllegalArgumentException("Label not recognised: $label")
        }

    //@Override
    var chosenIndex: Int
        get() = chosenOptionIndex
        set(index) {
            if (index < 0 || index >= optionLabels.size) {
                throw IndexOutOfBoundsException()
            }
            chosenOptionIndex = index
        }

    //public JComponent getEditComponent() {
    //    return new MultiChoiceOptionEditComponent(this);
    //}
    companion object {
        private const val serialVersionUID = 1L
    }

    init {
        require(optionLabels.size == optionDescriptions.size) { "Labels/descriptions mismatch." }
        this.optionLabels = optionLabels.clone()
        this.optionDescriptions = optionDescriptions.clone()
        this.defaultOptionIndex = defaultOptionIndex
        resetToDefault()
    }
}