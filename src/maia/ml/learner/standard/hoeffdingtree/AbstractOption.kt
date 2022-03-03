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
 * Abstract option.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
abstract class AbstractOption(name: String?, cliChar: Char, purpose: String?) :
    Option {
    /** Name of this option.  */
    override lateinit var name: String
        protected set

    /** Command line interface text of this option.  */
    override var cLIChar: Char = '\u0000'
        get() {
            TODO()
        }
        protected set

    /** Text of the purpose of this option.  */
    override lateinit var purpose: String
        protected set

    override fun resetToDefault() {
        setValueViaCLIString(defaultCLIString)
    }

    override val stateString: String?
        get() = valueAsCLIString


    override fun copy(): Option {
        return try {
            SerializeUtils.copyObject(this) as Option
        } catch (e: Exception) {
            throw RuntimeException("Object copy failed.", e)
        }
    } //@Override

    //public Option copy() {
    //    return (Option) super.copy();
    //}
    //@Override
    //public void getDescription(StringBuilder sb, int indent) {
    // TODO Auto-generated method stub
    //}
    //@Override
    //public JComponent getEditComponent() {
    //    return new StringOptionEditComponent(this);
    //}
    companion object {
        /** Array of characters not valid to use in option names.  */
        val illegalNameCharacters = charArrayOf(
            ' ', '-',
            '(', ')'
        )

        /**
         * Gets whether the name is valid or not.
         *
         * @param optionName the name of the option
         * @return true if the name that not contain any illegal character
         */
        fun nameIsLegal(optionName: String): Boolean {
            for (illegalChar in illegalNameCharacters) {
                if (optionName.indexOf(illegalChar) >= 0) {
                    return false
                }
            }
            return true
        }
    }

    /**
     * Creates a new instance of an abstract option given its class name,
     * command line interface text and its purpose.
     *
     * @param name the name of this option
     * @param cliChar the command line interface text
     * @param purpose the text describing the purpose of this option
     */
    init {
        name?.let { nameIsLegal(it) }?.let { require(it) { "Illegal option name: $name" } }
        if (name != null) {
            this.name = name
        }
        cLIChar = cliChar
        if (purpose != null) {
            this.purpose = purpose
        }
    }
}