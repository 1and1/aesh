/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.cl;

import org.jboss.aesh.cl.completer.CompleterData;
import org.jboss.aesh.cl.completer.DefaultValueOptionCompleter;
import org.jboss.aesh.cl.completer.FileOptionCompleter;
import org.jboss.aesh.cl.exception.CommandLineParserException;
import org.jboss.aesh.cl.internal.OptionInt;
import org.jboss.aesh.complete.CompleteOperation;
import org.jboss.aesh.console.Command;
import org.jboss.aesh.util.LoggerUtil;
import org.jboss.aesh.util.Parser;

import java.util.logging.Logger;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class CommandLineCompletionParser {

    private CommandLineParser parser;

    private static Logger logger = LoggerUtil.getLogger(CommandLineCompletionParser.class.getName());

    public CommandLineCompletionParser(CommandLineParser parser) {
        this.parser = parser;
    }


    /**
     * 1. find the last "word"
     *   if it starts with '-', we need to check if its a value or name
     * @param line buffer
     * @return ParsedCompleteObject
     */
    public ParsedCompleteObject findCompleteObject(String line) throws CommandLineParserException {
        //first we check if it could be a param
        if(Parser.findIfWordEndWithSpace(line)) {
            //check if we try to complete just after the command name
            if(line.trim().equals(parser.getCommand().getName())) {
                if(parser.getCommand().getArgument() == null) {
                    //basically an empty string except command name
                    return new ParsedCompleteObject(true, "", 0);
                }
                return new ParsedCompleteObject(null, "", parser.getCommand().getArgument().getType(), false);
            }

            //else we try to complete an option,an option value or arguments
            String lastWord = Parser.findEscapedSpaceWordCloseToEnd(line.trim());
            if(lastWord.startsWith("-")) {
                int offset = lastWord.length();
                while(lastWord.startsWith("-"))
                    lastWord = lastWord.substring(1);
                if(lastWord.length() == 0)
                    return new ParsedCompleteObject(false, null, offset);
                else if(parser.getCommand().findOption(lastWord) != null ||
                        parser.getCommand().findLongOption(lastWord) != null)
                    return findCompleteObjectValue(line, true);
                else
                    return new ParsedCompleteObject(false, null, offset);
            }
            else
                return new ParsedCompleteObject(true);
        }
        else
            return optionFinder(line);
    }

    private ParsedCompleteObject optionFinder(String line) throws CommandLineParserException {
        String lastWord = Parser.findEscapedSpaceWordCloseToEnd(line);
        //last word might be an option
        if(lastWord.startsWith("-") ) {
            String secLastWord =
                    Parser.findEscapedSpaceWordCloseToEnd(
                            line.substring(0,line.length()-lastWord.length()));
            //second to last word also start with -
            if(secLastWord.startsWith("-")) {
                //do this for now
                return findCompleteObjectValue(line, false);
            }
            //the last word is an option (most likely)
            else {
                if(lastWord.equals("-")) {
                    return new ParsedCompleteObject(true, "", 1);
                }
                else if(lastWord.equals("--")) {
                    return new ParsedCompleteObject(true, "", 2);
                }
                else {
                    //we have a complete shortName
                    if(!lastWord.startsWith("--") && lastWord.length() == 2)
                        return new ParsedCompleteObject(true,
                                Parser.trimOptionName(lastWord), lastWord.length(), true);
                    else {
                        String optionName = Parser.trimOptionName(lastWord);
                        if(parser.getCommand().hasLongOption(optionName))
                            return new ParsedCompleteObject(true, optionName, lastWord.length(), true);
                        else
                            return new ParsedCompleteObject(true, optionName, lastWord.length(), false);
                    }
                }
            }
        }
        else
            return findCompleteObjectValue(line, false);
    }

    /**
     * Only called when we know that the last word is an option value
     * If endsWithSpace is true we set the value to an empty string to indicate a value
     */
    private ParsedCompleteObject findCompleteObjectValue(String line, boolean endsWithSpace) throws CommandLineParserException {
        CommandLine cl = parser.parse(line, true);
        //the last word is an argument
        if(cl.getArgument() != null && !cl.getArgument().getValues().isEmpty()) {
            return new ParsedCompleteObject("",
                    cl.getArgument().getValues().get(cl.getArgument().getValues().size() - 1),
                    cl.getArgument().getType(), false);
        }
        //get the last option
        else {
            OptionInt po = cl.getOptions().get(cl.getOptions().size()-1);
            if(po.isLongNameUsed() || (po.getShortName() == null || po.getShortName().length() < 1))
                return new ParsedCompleteObject(po.getName(), endsWithSpace ? "" : po.getValue(), po.getType(), true);
            else
                return new ParsedCompleteObject( po.getShortName(), endsWithSpace ? "" : po.getValue(), po.getType(), true);
        }
    }

    public void injectValuesAndComplete(ParsedCompleteObject completeObject, Command command,
                                                 CompleteOperation completeOperation) {

        if(completeObject.doDisplayOptions()) {
            //got the whole name, just add a space
            if(completeObject.isCompleteOptionName()) {
                completeOperation.addCompletionCandidate("");
            }
            else {
                try {
                    parser.parse(completeOperation.getBuffer(), true, true);
                }
                catch (CommandLineParserException e) {
                   //ignored, shouldnt happen
                }
                //we have partial/full name
                if(completeObject.getName() != null && completeObject.getName().length() > 0) {
                    if(parser.getCommand().findPossibleLongNamesWitdDash(completeObject.getName()).size() > 0) {
                        //only one param
                        if(parser.getCommand().findPossibleLongNamesWitdDash(completeObject.getName()).size() == 1) {

                            completeOperation.addCompletionCandidate(
                                    parser.getCommand().findPossibleLongNamesWitdDash(completeObject.getName()).get(0));
                            completeOperation.setOffset( completeOperation.getCursor() - 2 - completeObject.getName().length());
                        }
                        //multiple params
                        else {
                            completeOperation.addCompletionCandidates(parser.getCommand().findPossibleLongNamesWitdDash(completeObject.getName()));
                        }

                    }
                }
                else {
                    if(parser.getCommand().getOptionLongNamesWithDash().size() > 1)
                        completeOperation.addCompletionCandidates(parser.getCommand().getOptionLongNamesWithDash());
                    else if(parser.getCommand().getOptionLongNamesWithDash().size() == 1) {
                        int count = 0;
                        while(completeOperation.getBuffer().substring(0, completeOperation.getBuffer().length()-count).endsWith("-"))
                            count++;
                        completeOperation.addCompletionCandidate(parser.getCommand().getOptionLongNamesWithDash().get(0));
                        completeOperation.setOffset( completeOperation.getCursor() - count);
                    }

                }
            }
        }
        //complete option value
        else if(completeObject.isOption()) {
            OptionInt currentOption = parser.getCommand().findOption(completeObject.getName());
            if(currentOption == null)
                currentOption = parser.getCommand().findLongOption(completeObject.getName());

            //split the line on the option name. populate the object, then call the options completer
            String displayName = currentOption.getDisplayName();
            //this shouldnt happen
            if(displayName == null) {
                return;
            }
            String rest = completeOperation.getBuffer().substring(0, completeOperation.getBuffer().lastIndexOf( displayName));

            try {
                parser.populateObject(command, rest);
            }
            //this should be ignored at some point
            catch (CommandLineParserException e) {
                e.printStackTrace();
            }

            if(currentOption.getCompleter() != null) {
                CompleterData completions = currentOption.getCompleter().complete(completeObject.getValue());
                completeOperation.addCompletionCandidates(completions.getCompleterValues());

                if(completions.getCompleterValues().size() == 1) {
                    if(currentOption.getCompleter() instanceof FileOptionCompleter)
                        completeOperation.setOffset( completeOperation.getCursor());
                    else
                        completeOperation.setOffset( completeOperation.getCursor() - completeObject.getValue().length());

                    completeOperation.doAppendSeparator( completions.isAppendSpace());
                }
            }
            //only try to complete default values if completer is null
            else if(currentOption.getDefaultValues().size() > 0) {
                CompleterData completions =
                        new DefaultValueOptionCompleter(currentOption.getDefaultValues()).complete(completeObject.getValue());
                completeOperation.addCompletionCandidates(completions.getCompleterValues());

                if(completions.getCompleterValues().size() == 1) {
                    if(currentOption.getCompleter() instanceof FileOptionCompleter)
                        completeOperation.setOffset( completeOperation.getCursor());
                    else
                        completeOperation.setOffset( completeOperation.getCursor() - completeObject.getValue().length());

                    completeOperation.doAppendSeparator( completions.isAppendSpace());
                }
            }
        }
        else if(completeObject.isArgument()) {
            String lastWord = Parser.findEscapedSpaceWordCloseToEnd(completeOperation.getBuffer());
            String rest = completeOperation.getBuffer().substring(0, completeOperation.getBuffer().length() - lastWord.length());
            try {
                parser.populateObject(command, rest);
            } catch (CommandLineParserException e) {
                e.printStackTrace();
            }
            if(parser.getCommand().getArgument() != null &&
                    parser.getCommand().getArgument().getCompleter() != null) {
                CompleterData completions = parser.getCommand().getArgument().getCompleter().complete(completeObject.getValue());
                completeOperation.addCompletionCandidates(completions.getCompleterValues());

                if(completions.getCompleterValues().size() == 1) {
                    if(parser.getCommand().getArgument().getCompleter() instanceof FileOptionCompleter)
                        completeOperation.setOffset( completeOperation.getCursor());
                    else
                        completeOperation.setOffset( completeOperation.getCursor() - completeObject.getValue().length());

                    completeOperation.doAppendSeparator( completions.isAppendSpace());
                }

            }
            else if(parser.getCommand().getArgument() != null &&
                    parser.getCommand().getArgument().getDefaultValues().size() > 0) {
                CompleterData completions =
                        new DefaultValueOptionCompleter(
                                parser.getCommand().getArgument().getDefaultValues()).complete(completeObject.getValue());
                completeOperation.addCompletionCandidates(completions.getCompleterValues());

                if(completions.getCompleterValues().size() == 1) {
                    if(parser.getCommand().getArgument().getCompleter() instanceof FileOptionCompleter)
                        completeOperation.setOffset( completeOperation.getCursor());
                    else
                        completeOperation.setOffset( completeOperation.getCursor() - completeObject.getValue().length());

                    completeOperation.doAppendSeparator( completions.isAppendSpace());
                }
                completeOperation.doAppendSeparator( completions.isAppendSpace());
            }
        }
    }

}
