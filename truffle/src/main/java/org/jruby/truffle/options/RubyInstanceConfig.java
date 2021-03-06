/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007-2011 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2009 Joseph LaFata <joe@quibb.org>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.truffle.options;

import com.oracle.truffle.api.TruffleOptions;
import org.jruby.truffle.core.string.KCode;
import org.jruby.truffle.language.control.JavaException;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * A structure used to configure new JRuby instances. All publicly-tweakable
 * aspects of Ruby can be modified here, including those settable by command-
 * line options, those available through JVM properties, and those suitable for
 * embedding.
 */
@SuppressWarnings("unused")
public class RubyInstanceConfig {

    public RubyInstanceConfig() {
        currentDirectory = System.getProperty("user.dir", "/");
        environment = new HashMap<>();
        environment.putAll(System.getenv());
    }

    public void processArguments(String[] arguments) {
        final ArgumentProcessor processor = new ArgumentProcessor(arguments, this);
        processor.processArguments();
        processArgumentsWithRubyopts();
        if (!TruffleOptions.AOT && !hasScriptArgv && !usePathScript && System.console() != null) {
            setUsePathScript("irb");
        }
    }

    public void processArgumentsWithRubyopts() {
        // environment defaults to System.getenv normally
        Object rubyoptObj = environment.get("RUBYOPT");
        String rubyopt = rubyoptObj == null ? null : rubyoptObj.toString();

        if (rubyopt == null || rubyopt.length() == 0) return;

        String[] rubyoptArgs = rubyopt.split("\\s+");
        if (rubyoptArgs.length != 0) {
            new ArgumentProcessor(rubyoptArgs, false, true, true, this).processArguments();
        }
    }

    public byte[] inlineScript() {
        return inlineScript.toString().getBytes();
    }

    public InputStream getScriptSource() {
        try {
            // KCode.NONE is used because KCODE does not affect parse in Ruby 1.8
            // if Ruby 2.0 encoding pragmas are implemented, this will need to change
            if (hasInlineScript) {
                return new ByteArrayInputStream(inlineScript());
            } else if (isForceStdin() || getScriptFileName() == null) {
                return System.in;
            } else {
                final String script = getScriptFileName();
                return new FileInputStream(script);
            }
        } catch (IOException e) {
            throw new JavaException(e);
        }
    }

    public String displayedFileName() {
        if (hasInlineScript) {
            if (scriptFileName != null) {
                return scriptFileName;
            } else {
                return "-e";
            }
        } else if (usePathScript) {
            return "-S";
        } else if (isForceStdin() || getScriptFileName() == null) {
            return "-";
        } else {
            return getScriptFileName();
        }
    }

    public PrintStream getOutput() {
        return output;
    }

    public PrintStream getError() {
        return error;
    }

    public void setCurrentDirectory(String newCurrentDirectory) {
        currentDirectory = newCurrentDirectory;
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    public String[] getArgv() {
        return argv;
    }

    public void setArgv(String[] argv) {
        this.argv = argv;
    }

    public StringBuffer getInlineScript() {
        return inlineScript;
    }

    public void setHasInlineScript(boolean hasInlineScript) {
        this.hasScriptArgv = true;
        this.hasInlineScript = hasInlineScript;
    }

    public Collection<String> getRequiredLibraries() {
        return requiredLibraries;
    }

    public List<String> getLoadPaths() {
        return loadPaths;
    }

    public void setShouldPrintUsage(boolean shouldPrintUsage) {
        this.shouldPrintUsage = shouldPrintUsage;
    }

    public boolean getShouldPrintUsage() {
        return shouldPrintUsage;
    }

    public boolean isInlineScript() {
        return hasInlineScript;
    }

    /**
     * True if we are only using source from stdin and not from a -e or file argument.
     */
    public boolean isForceStdin() {
        return forceStdin;
    }

    /**
     * Set whether we should only look at stdin for source.
     */
    public void setForceStdin(boolean forceStdin) {
        this.forceStdin = forceStdin;
    }

    public void setScriptFileName(String scriptFileName) {
        this.hasScriptArgv = true;
        this.scriptFileName = scriptFileName;
    }

    public String getScriptFileName() {
        return scriptFileName;
    }

    public void setAssumeLoop(boolean assumeLoop) {
        this.assumeLoop = assumeLoop;
    }

    public void setAssumePrinting(boolean assumePrinting) {
        this.assumePrinting = assumePrinting;
    }

    public void setProcessLineEnds(boolean processLineEnds) {
        this.processLineEnds = processLineEnds;
    }

    public void setSplit(boolean split) {
        this.split = split;
    }

    public boolean isSplit() {
        return split;
    }

    public void setVerbosity(Verbosity verbosity) {
        this.verbosity = verbosity;
    }

    public boolean isVerbose() {
        return verbosity == Verbosity.TRUE;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setShowVersion(boolean showVersion) {
        this.showVersion = showVersion;
    }

    public boolean isShowVersion() {
        return showVersion;
    }

    public void setShowCopyright(boolean showCopyright) {
        this.showCopyright = showCopyright;
    }

    public boolean isShowCopyright() {
        return showCopyright;
    }

    public void setShouldRunInterpreter(boolean shouldRunInterpreter) {
        this.shouldRunInterpreter = shouldRunInterpreter;
    }

    public boolean getShouldRunInterpreter() {
        return shouldRunInterpreter && (hasScriptArgv || !showVersion);
    }

    public void setShouldCheckSyntax(boolean shouldSetSyntax) {
        this.shouldCheckSyntax = shouldSetSyntax;
    }

    public boolean getShouldCheckSyntax() {
        return shouldCheckSyntax;
    }

    public void setInputFieldSeparator(String inputFieldSeparator) {
        this.inputFieldSeparator = inputFieldSeparator;
    }

    public void setInternalEncoding(String internalEncoding) {
        this.internalEncoding = internalEncoding;
    }

    public String getInternalEncoding() {
        return internalEncoding;
    }

    public void setExternalEncoding(String externalEncoding) {
        this.externalEncoding = externalEncoding;
    }

    public String getExternalEncoding() {
        return externalEncoding;
    }

    public void setInPlaceBackupExtension(String inPlaceBackupExtension) {
        this.inPlaceBackupExtension = inPlaceBackupExtension;
    }

    public String getInPlaceBackupExtension() {
        return inPlaceBackupExtension;
    }

    public Map<String, String> getOptionGlobals() {
        return optionGlobals;
    }

    public boolean isArgvGlobalsOn() {
        return argvGlobalsOn;
    }

    public void setArgvGlobalsOn(boolean argvGlobalsOn) {
        this.argvGlobalsOn = argvGlobalsOn;
    }

    public boolean isDisableGems() {
        return disableGems;
    }

    public void setDisableGems(boolean dg) {
        this.disableGems = dg;
    }

    public void setXFlag(boolean xFlag) {
        this.xFlag = xFlag;
    }

    public boolean isXFlag() {
        return xFlag;
    }

    public boolean isFrozenStringLiteral() {
        return frozenStringLiteral;
    }

    public void setFrozenStringLiteral(boolean frozenStringLiteral) {
        this.frozenStringLiteral = frozenStringLiteral;
    }

    /**
     * Indicates whether the script must be extracted from script source
     */
    private boolean xFlag = false;

    /**
     * Indicates whether the script has a shebang line or not
     */
    private PrintStream output         = System.out;
    private PrintStream error          = System.err;

    private String currentDirectory;

    /** Environment variables; defaults to System.getenv() in constructor */
    private Map<String, String> environment;
    private String[] argv = {};

    private String internalEncoding = null;
    private String externalEncoding = null;

    // from CommandlineParser
    private List<String> loadPaths = new ArrayList<>();
    private StringBuffer inlineScript = new StringBuffer();
    private boolean hasInlineScript = false;
    private boolean usePathScript = false;
    private String scriptFileName = null;
    private Collection<String> requiredLibraries = new LinkedHashSet<>();
    private boolean argvGlobalsOn = false;
    private boolean assumeLoop = false;
    private boolean assumePrinting = false;
    private Map<String, String> optionGlobals = new HashMap<>();
    private boolean processLineEnds = false;
    private boolean split = false;
    private Verbosity verbosity = Verbosity.FALSE;
    private boolean debug = false;
    private boolean showVersion = false;
    private boolean showCopyright = false;
    private boolean shouldRunInterpreter = true;
    private boolean shouldPrintUsage = false;
    private boolean shouldPrintProperties=false;
    private boolean shouldCheckSyntax = false;
    private String inputFieldSeparator = null;
    private String inPlaceBackupExtension = null;
    private boolean disableGems = false;
    private boolean hasScriptArgv = false;
    private boolean frozenStringLiteral = false;
    private KCode kcode;
    private String sourceEncoding;

    private boolean forceStdin = false;

    public Verbosity getVerbosity() {
        return verbosity;
    }

    public void setKCode(KCode kcode) {
        this.kcode = kcode;
    }

    public KCode getKCode() {
        return kcode;
    }

    public void setSourceEncoding(String sourceEncoding) {
        this.sourceEncoding = sourceEncoding;
    }

    public void setUsePathScript(String name) {
        scriptFileName = name;
        usePathScript = true;
    }

    public boolean shouldUsePathScript() {
        return usePathScript;
    }

}
