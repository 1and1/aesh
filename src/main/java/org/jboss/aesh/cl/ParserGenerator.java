/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.OptionInt;
import org.jboss.aesh.cl.internal.ParameterInt;

/**
 * Generates a {@link CommandLineParser} based on annotations defined in
 * the specified class.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ParserGenerator {

    public static CommandLineParser generateParser(Class<?>... clazzes) throws CommandLineParserException {

        ParserBuilder builder = new ParserBuilder();
        for(Class clazz : clazzes) {

        Parameter param = (Parameter) clazz.getAnnotation(Parameter.class);
        if(param == null)
            throw new CommandLineParserException("Can only create parser from class thats annotated with Parameter");
            builder.addParameter(generateParameter(param));
        }
        return builder.generateParser();
    }

    private static ParameterInt generateParameter(Parameter param) throws CommandLineParserException {
        if(param.name() == null || param.name().length() < 1)
            throw new CommandLineParserException("The parameter name must be defined");

        if(param.options() != null) {
            OptionInt[] options = new OptionInt[param.options().length];
            for(int i=0; i < param.options().length; i++) {
                Option o = param.options()[i];
                options[i] =  new OptionInt(
                        o.name(), o.longName(), o.description(),
                        o.hasValue(), o.argument(), o.required(), o.valueSeparator(),
                        o.isProperty(), o.hasMultipleValues(), o.type());
            }

            return new ParameterInt(param.name(), param.usage(),
                    param.argumentType(), options);
        }
        else
            return new ParameterInt(param.name(), param.usage(),
                    param.argumentType(), new OptionInt[0]);
    }

}
