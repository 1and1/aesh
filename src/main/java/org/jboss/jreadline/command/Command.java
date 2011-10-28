/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.jreadline.command;

/**
 * Common interface to integrate a command to JReadline
 *
 * @author Ståle W. Pedersen <stale.pedersen@jboss.org>
 */
public interface Command {

    /**
     * Fast check if the current line matches a command
     *
     * @param line command line
     * @return true if it matches the current command
     */
    boolean matchCommand(String line);

    /**
     * Execute command and return the output
     *
     * @param line command line
     * @return output of command that will be pushed to console
     */
    String executeCommand(String line);
}
