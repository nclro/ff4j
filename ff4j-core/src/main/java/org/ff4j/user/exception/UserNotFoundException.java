package org.ff4j.user.exception;

/*-
 * #%L
 * ff4j-core
 * %%
 * Copyright (C) 2013 - 2019 FF4J
 * %%
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
 * #L%
 */

import org.ff4j.core.exception.NotFoundException;

/**
 * Store could be parameterized to through exception when Property not found.
 * 
 * @author Cedrick Lunven (@clunven)
 */
public class UserNotFoundException extends NotFoundException {
    
    /** Serial. */
    private static final long serialVersionUID = 2815266262763976882L;

    /**
     * Parameterized constructor.
     * 
     * @param propertyName
     *            property to be processed
     **/
    public UserNotFoundException(String uid) {
        super(uid);
    }
    
    /**
     * Parameterized constructor.
     * 
     * @param featureName
     *            feature to be processed
     **/
    public UserNotFoundException(String uid, Throwable parentException) {
        super(uid, parentException);
    }
    
}
