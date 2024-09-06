package org.mozilla.javascript;

/**
 * Interface to define methods for Parser to interact with TokenStream.
 */
public interface IParser {
    /**
     * Reports an error found in the TokenStream.
     *
     * @param message The error message.
     */
	void addError(String string);

	/**
     * Reports an error found in the TokenStream.
     *
     * @param message The error message.
     */
	void reportError(String string);

	void addError(String string, int c);

	boolean inUseStrictDirective();

	void addWarning(String string, String object);

	void reportError(String string, String valueOf);

	boolean getCalledByCompileFunction();
	
	CompilerEnvirons getCompilerEnv();
}
