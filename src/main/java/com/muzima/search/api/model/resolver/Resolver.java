/**
 * Copyright 2012 Muzima Team
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

package com.muzima.search.api.model.resolver;

import java.io.IOException;
import java.net.HttpURLConnection;

public interface Resolver {

    /**
     * Return the full REST resource based on the search string passed to the method.
     *
     * @param searchString the search string.
     * @return full uri to the REST resource.
     */
    String resolve(final String searchString) throws IOException;

    /**
     * Add authentication information to the http url connection.
     *
     * @param connection the original connection without authentication information.
     * @return the connection with authentication information when applicable.
     */
    HttpURLConnection authenticate(final HttpURLConnection connection) throws IOException;
}
