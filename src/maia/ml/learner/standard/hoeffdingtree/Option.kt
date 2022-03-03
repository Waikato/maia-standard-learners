package maia.ml.learner.standard.hoeffdingtree

import java.io.Serializable

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
 * Interface representing an option or parameter.
 *
 * @author Richard Kirkby (rkirkby@cs.waikato.ac.nz)
 * @version $Revision: 7 $
 */
interface Option : Serializable {
    /**
     * Gets the name of this option
     *
     * @return the name of this option
     */
    val name: String?

    /**
     * Gets the Command Line Interface text of this option
     *
     * @return the Command Line Interface text
     */
    val cLIChar: Char

    /**
     * Gets the purpose of this option
     *
     * @return the purpose of this option
     */
    val purpose: String?

    /**
     * Gets the Command Line Interface text
     *
     * @return the Command Line Interface text
     */
    val defaultCLIString: String?

    /**
     * Sets value of this option via the Command Line Interface text
     *
     * @param s the Command Line Interface text
     */
    fun setValueViaCLIString(s: String?)

    /**
     * Gets the value of a Command Line Interface text as a string
     *
     * @return the string with the value of the Command Line Interface text
     */
    val valueAsCLIString: String?

    /**
     * Resets this option to the default value
     *
     */
    fun resetToDefault()

    /**
     * Gets the state of this option in human readable form
     *
     * @return the string with state of this option in human readable form
     */
    val stateString: String?

    /**
     * Gets a copy of this option
     *
     * @return the copy of this option
     */
    fun copy(): Option?
    /**
     * Gets the GUI component to edit
     *
     * @return the component to edit
     */
    //public JComponent getEditComponent();
}