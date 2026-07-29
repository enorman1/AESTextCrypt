/**
 * Copyright 2013 Chris Wood
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

package com.ceperman.textcrypt;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

/*************************************
 * Class Version - METHODS & PROPERTIES *
 ************************************/
public class Version {
	private static final String BUNDLE_NAME = "com.ceperman.textcrypt.version"; // file [messages.properties]
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);
	/**
	 * Get version text string
	 */
	public static String get() {
		try {
			return RESOURCE_BUNDLE.getString("version");
		} catch (MissingResourceException e) {
			return "0.0";
		}
	}
}
