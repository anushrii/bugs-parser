package bugs;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;

/**
 * This class consists of a number of methods that "recognize" strings
 * composed of Tokens that follow the indicated grammar rules for each
 * method.
 * <p>Each method may have one of three outcomes:
 * <ul>
 *   <li>The method may succeed, returning <code>true</code> and
 *      consuming the tokens that make up that particular nonterminal.</li>
 *   <li>The method may fail, returning <code>false</code> and not
 *       consuming any tokens.</li>
 *   <li>(Some methods only) The method may determine that an
 *       unrecoverable error has occurred and throw a
 *       <code>SyntaxException</code></li>.
 * </ul>
 * @author David Matuszek
 * @version February 2015
 */
public class Recognizer {
    StreamTokenizer tokenizer = null;
    int lineNumber;
    
    /**
     * Constructs a Recognizer for the given string.
     * @param text The string to be recognized.
     */
    public Recognizer(String text) {
        Reader reader = new StringReader(text);
        tokenizer = new StreamTokenizer(reader);
        tokenizer.parseNumbers();
        tokenizer.eolIsSignificant(true);
        tokenizer.slashStarComments(true);
        tokenizer.slashSlashComments(true);
        tokenizer.lowerCaseMode(false);
        tokenizer.ordinaryChars(33, 47);
        tokenizer.ordinaryChars(58, 64);
        tokenizer.ordinaryChars(91, 96);
        tokenizer.ordinaryChars(123, 126);
        tokenizer.quoteChar('\"');
        lineNumber = 1;
    }

    /**
     * Tries to build an &lt;expression&gt;.
     * <pre>&lt;expression&gt; ::= &lt;arithmetic expression&gt; { &lt;comparator&gt; &lt;arithmetic expression&gt; }</pre>
     * A <code>SyntaxException</code> will be thrown if the comparator
     * is present but not followed by a valid &lt;arithmetic expression&gt;.
     * @return <code>true</code> if an expression is recognized.
     */
    public boolean isExpression() {
       
        if(!isArithmeticExpression()) return false;
        while(isComparator()){
        	if(!isArithmeticExpression())
        		error("Error expression not found after comparator");
        }
        return true;
    }

    /**
     * Tries to build an &lt;arithmetic expression&gt; on the global stack.
     * <pre>&lt;arithmetic expression&gt; ::= &lt;term&gt; { &lt;add_operator&gt; &lt;expression&gt; }</pre>
     * A <code>SyntaxException</code> will be thrown if the add_operator
     * is present but not followed by a valid &lt;expression&gt;.
     * @return <code>true</code> if an expression is recognized.
     */
    public boolean isArithmeticExpression() {
        boolean startsWithUnary = symbol("+") || symbol("-"); // will be used late
        if (!isTerm())
            return false;
        while (isAddOperator()) {
            if (!isTerm()) error("Error in expression after '+' or '-'");
        }
        return true;
    }

    /**
     * Tries to recognize a &lt;term&gt;.
     * <pre>&lt;term&gt; ::= &lt;factor&gt; { &lt;multiply_operator&gt; &lt;term&gt;}</pre>
     * A <code>SyntaxException</code> will be thrown if the multiply_operator
     * is present but not followed by a valid &lt;term&gt;.
     * @return <code>true</code> if a term is recognized.
     */
    public boolean isTerm() {
        if (!isFactor()) return false;
        while (isMultiplyOperator()) {
            if (!isTerm()) error("No term after '*' or '/'");
        }
        return true;
    }

    /**
     * Tries to recognize a &lt;factor&gt;.
     * <pre>&lt;factor&gt; ::= [ &lt;add operator&gt; ] &lt;unsigned factor&gt;</pre>
     * @return <code>true</code> if a factor is parsed.
     */
    public boolean isFactor() {
        if(symbol("+") || symbol("-")) {
            if (isUnsignedFactor()) {
                return true;
            }
            error("No factor following unary plus or minus");
            return false; // Can't ever get here
        }
        return isUnsignedFactor();
    }

    /**
     * Tries to recognize an &lt;unsigned factor&gt;.
     * <pre>&lt;factor&gt; ::= &lt;name&gt; "." &lt;name&gt;
     *           | &lt;name&gt; "(" &lt;parameter list&gt; ")"
     *           | &lt;name&gt;
     *           | &lt;number&gt;
     *           | "(" &lt;expression&gt; ")"</pre>
     * A <code>SyntaxException</code> will be thrown if the opening
     * parenthesis is present but not followed by a valid
     * &lt;expression&gt; and a closing parenthesis.
     * @return <code>true</code> if a factor is recognized.
     */
    public boolean isUnsignedFactor() {
        if (isVariable()) {
            if (symbol(".")) {              // reference to another Bug
                if (name()) return true;
                error("Incorrect use of dot notation");
            }
            else if (isParameterList()) return true; // function call
            else return true;                        // just a variable
        }
        if (number()) return true;
        if (symbol("(")) {
            if (!isExpression()) error("Error in parenthesized expression");
            if (!symbol(")")) error("Unclosed parenthetical expression");
            return true;
       }
       return false;
    }

    /**
     * Tries to recognize a &lt;parameter list&gt;.
     * <pre>&ltparameter list&gt; ::= "(" [ &lt;expression&gt; { "," &lt;expression&gt; } ] ")"
     * @return <code>true</code> if a parameter list is recognized.
     */
    public boolean isParameterList() {
        if (!symbol("(")) return false;
        if (isExpression()) {
            while (symbol(",")) {
                if (!isExpression()) error("No expression after ','");
            }
        }
        if (!symbol(")")) error("Parameter list doesn't end with ')'");
        return true;
    }

    /**
     * Tries to recognize an &lt;add_operator&gt;.
     * <pre>&lt;add_operator&gt; ::= "+" | "-"</pre>
     * @return <code>true</code> if an addop is recognized.
     */
    public boolean isAddOperator() {
        return symbol("+") || symbol("-");
    }

    /**
     * Tries to recognize a &lt;multiply_operator&gt;.
     * <pre>&lt;multiply_operator&gt; ::= "*" | "/"</pre>
     * @return <code>true</code> if a multiply_operator is recognized.
     */
    public boolean isMultiplyOperator() {
        return symbol("*") || symbol("/");
    }

    /**
     * Tries to recognize a &lt;variable&gt;.
     * <pre>&lt;variable&gt; ::= &lt;NAME&gt;</pre>
     * @return <code>true</code> if a variable is recognized.
     */
    public boolean isVariable() {
        return name();
    }
    /**
     * Tries to recognize a &lt;comparator&gt;.
     * <pre>&lt;comparator&gt; ::= "<" | "<="| "="| "!="| ">="| ">" </pre>
     * @return <code>true</code> if a comparator is recognized.
     */
    public boolean isComparator() {
    	if(symbol("<")||symbol(">")||symbol("!")){
    		if(symbol("=")) return true;
    		else return true;
    	}
    	else if(symbol("=")){
    		return true;
    	}
    	return false;
    		
	//	return symbol("<")||symbol(">")||symbol(">=")||symbol("<=")||symbol("=")||symbol("!=");
    	
    }
    /**
     * Tries to recognize a &lt;program&gt;.
     * <pre>&lt;program&gt; ::= [ &lt; allbugs code&gt;] &lt;bugs definition&gt;{ &lt;bugs definition&gt;}</pre>
     * A <code>SyntaxException</code> will be thrown if an allbugs code is present not followed by  a &lt;bugs definition&gt;
     * @return <code>true</code> if a program is recognized.
     */
    public boolean isProgram(){
    	if(isAllbugsCode()){
    		if(!isBugDefinition()) error("Bugs Definition not found");
    	}
    	else {
    	if(!isBugDefinition()) return false;
    	}
    	while(true){
    		if(isBugDefinition()) continue;
    		else  break;
    	}
    	if(!eof()) error("Early terminaton of the program, end of program not recognized");
    	return true;
    }
    /**
     * Tries to recognize an &lt;allbugs code&gt;.
     * <pre> &lt;allbugs code&gt; ::= "Allbugs" "{" &lt;eol&gt;
     *                                    { &lt;var declaration&gt;}
     *                                    { &lt;function definition&gt;} 
     *                                    "}" &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "Allbugs"  is present not 
     * followed by an open braces and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if an allbugs code is recognized.
     */
    
    public boolean isAllbugsCode(){
    	if(!keyword("Allbugs")) return false;
    	if(!symbol("{")) error(" Missing open braces '{'");
    	if(!isEol()) error("Syntax error, end of line not found");
    	while(true){
    		if(isVarDeclaration()) continue;
    		else  break;
    	}
    	while(true){
    		if(isFunctionDefinition()) continue;
    		else  break;
    	}
    	if(!symbol("}")) error("Missing closed braces '}'");
    	if(!isEol()) error("Syntax error, end of line not found");
    	return true;
    }
    /**
     * Tries to recognize a &lt;bugs definition&gt;.
     * <pre> &lt;bugs definition&gt; ::= "Bug" &lt;name&gt; "{" &lt;eol&gt;
     *                                       { &lt;var declaration&gt;}
     *                                       [{ &lt;initialization block&gt;}]
     *                                       { &lt;command&gt;}
     *                                       [{ &lt;command&gt;}]
     *                                       { &lt;function definition&gt;} 
     *                                       "}" &lt;eol&gt;</pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "Bug"  is present not 
     * followed by a name, open braces and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if a bug definition is recognized.
     */
    public boolean isBugDefinition(){
    	if(!keyword("Bug")) return false;
    	if(!name()) error("syntax error :  no bug name found");
    	if(!symbol("{")) error(" Missing open braces '{'");
    	if(!isEol()) error("Syntax error, end of line not found");
    	while(true){
    		if(isVarDeclaration()) continue;
    		else  break;
    	}
    	if(isInitializationBlock()){
    		if(!isCommand()) error("command not found");
    	}
   
    	else{
    	if(!isCommand()) error("command not found");
    	}
    	while(true){
    		if(isCommand()) continue;
    		else  break;
    	}
    	while(true){
    		if(isFunctionDefinition()) continue;
    		else  break;
    	}
    	if(!symbol("}")) error(" Missing close braces '{'");
    	if(!isEol()) error("Syntax error, end of line not found");
    	return true;
    }

    /**
     * Tries to recognize a &lt;var declaration&gt;.
     * <pre> &lt;var declaration&gt; ::= "var" &lt;name&gt; {"," &lt;name&gt;}  &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "var"  is present not 
     * followed by a name and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if a var definition is recognized.
     */
    public boolean isVarDeclaration(){
    	if(!keyword("var")) return false;
    	if(!name()) error("syntax error :  no variable name found");
    	while(symbol(",")){
    		if(!name()) error("Syntax error : no name after ','");
    	}
    	if(!isEol()) error("Syntax error, end of line not found");
    	return true;
    }
    
    /**
     * Tries to recognize a &lt;initialization block&gt;.
     * <pre> &lt;initialization block&gt; ::= "initially" &lt;block&gt;  </pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "initially"  is present not 
     * followed by a &lt;block&gt; 
     * @return <code>true</code> if a initialization block is recognized.
     */
    public boolean isInitializationBlock(){
    	if(!keyword("initially")) return false;
    	if(!isBlock()) error("syntax error :  no initializaton found");
    	return true;
    }
    /**
     * Tries to recognize a &lt;command&gt;.
     * <pre> &lt;command&gt; ::=  &lt;action&gt;
     *                            | &lt;statement &gt; </pre>
     * @return <code>true</code> if a initialization block is recognized.
     */
    
    public boolean isCommand(){
    	if(!(isAction()||isStatement())) return false;
    	return true;
    	
    }
    
    /**
     * Tries to recognize a &lt;statement&gt;.
     * <pre> &lt;statement&gt; ::=  &lt;assignment statement&gt;
     *                           | &lt;loop statement&gt; 
     *                           | &lt;exit if statement&gt;
     *                           | &lt;switch statement&gt;
     *                           | &lt;return statement&gt;
     *                           | &lt;do statement&gt;
     *                           | &lt;color statement&gt; </pre>
     * @return <code>true</code> if a ststement is recognized.
     */
    public boolean isStatement(){
    	if(!(isAssignmentStatement()||isLoopStatement()||isExitIfStatement()||isSwitchStatement()||isReturnStatement()||isDoStatement()||isColorStatement())) return false;
    	return true;
    }
    /**
     * Tries to recognize a &lt;action&gt;.
     * <pre> &lt;action&gt; ::=  &lt;move action &gt;
     *                        | &lt;moveto action &gt; 
     *                        | &lt;turn action &gt;
     *                        | &lt;turnto action &gt;
     *                        | &lt;line action &gt; </pre>
     * @return <code>true</code> if an action is recognized.
     */
    public boolean isAction(){
    	if(!(isMoveAction()||isMoveToAction()||isTurnAction()||isTurnToAction()||isLineAction())) return false;
    	return true;
    }
    /**
     * Tries to recognize a &lt;move action&gt;.
     * <pre>  &lt;move action&gt; ::= "move" &lt;expression&gt;  &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "move"  is present not 
     * followed by an expression and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if a move action is recognized.
     */
    
    public boolean isMoveAction(){
    	if(!keyword("move")) return false;
    	if(!isExpression()) error("Incomplete move action");
    	if(!isEol()) error("Syntax error, end of line not found");
    	return true;
    }
    /**
     * Tries to recognize a &lt;moveto action&gt;.
     * <pre>  &lt;moveto action&gt; ::= "moveto" &lt;expression&gt; "," &lt;expression&gt; &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "moveto"  is present not 
     * followed by an expression and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if a moveto action is recognized.
     */
    public boolean isMoveToAction(){
     	if(!keyword("moveto")) return false;
    	if(!isExpression()) error("Incomplete move action");
    	if(!symbol(",")) error("syntax error , expected ','");
    	if(!isExpression()) error("Incomplete move to action");
    	if(!isEol()) error("Syntax error, end of line not found ");
    	return true;
    }
    /**
     * Tries to recognize a &lt;turn action&gt;.
     * <pre>  &lt;turn action&gt; ::= "turn" &lt;expression&gt;  &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "turn"  is present not 
     * followed by an expression and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if a turn action is recognized.
     */
    public boolean isTurnAction(){
    	if(!keyword("turn")) return false;
    	if(!isExpression()) error("Incomplete move action");
    	if(!isEol()) error("Syntax error, end of line not found");
    	return true;
    }
    /**
     * Tries to recognize a &lt;turnto action&gt;.
     * <pre>  &lt;turnto action&gt; ::= "turnto" &lt;expression&gt;  &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "turnto"  is present not 
     * followed by an expression and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if a turnto action is recognized.
     */
    public boolean isTurnToAction(){
    	if(!keyword("turnto")) return false;
    	if(!isExpression()) error("Incomplete move action");
    	if(!isEol()) error("Syntax error, end of line not found");
    	return true;
    }
    /**
     * Tries to recognize a &lt;line action&gt;.
     * <pre>  &lt;line action&gt; ::= "line" &lt;expression&gt; "," &lt;expression&gt; "," &lt; expression&gt; "," &lt;expression&gt; &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "line"  is present not 
     * followed by an expression and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if a line action is recognized.
     */
    public boolean isLineAction(){
    	if(!keyword("line")) return false;
    	if(!isExpression()) error("Expression not found");
    	if(!symbol(",")) error("syntax error , expected ','");
    	if(!isExpression()) error("Expression not found");
    	if(!symbol(",")) error("syntax error , expected ','");
    	if(!isExpression()) error("Expression not found");
    	if(!symbol(",")) error("syntax error , expected ','");
    	if(!isExpression()) error("Expression not found");
    	if(!isEol()) error("Syntax error, end of line not found ");
    	return true;
    }
    /**
     * Tries to recognize a &lt;assignment statement&gt;.
     * <pre>  &lt;assignment statement&gt; ::= &lt;variable&gt; "=" expression&gt; &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if a vraiable  is present not 
     * followed by a "=" and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if an assignment statement is recognized.
     */
    public boolean isAssignmentStatement(){
    	if(!isVariable())return false;
    	if(!symbol("=")) error("Incomplete assignment statement, expected an expression");
     	if(!isExpression()) error("expression not found after assignment");
    	if(!isEol()) error("Syntax error, end of line not found");
    	return true;
    }
    /**
     * Tries to recognize a &lt;loop statement&gt;.
     * <pre>  &lt;loop statement&gt; ::= "loop" &lt;block&gt;   </pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "loop"  is present not 
     * followed by a block.
     * @return <code>true</code> if a loop statement action is recognized.
     */
    public boolean isLoopStatement(){
    	if(!keyword("loop")) return false;
    	if(!isBlock()) error("Incomplete loop");
    	return true;
    }
    /**
     * Tries to recognize a &lt;exit if statement&gt;.
     * <pre>  &lt;exit if statement&gt; ::= "exit" "if" &lt;expression&gt; &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if a  keyword "exit" is present but not 
     * followed by a keyword "if" and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if an exit if statement is recognized.
     */
    public boolean isExitIfStatement(){
    	if(!keyword("exit")) return false;
    	if(!keyword("if")) error("Syntax error");
    	if(!isExpression()) error("Expression not found after exitif ");
    	if(!isEol()) error("Syntax error, end of line not found");
    	return true;
    }
    /**
     * Tries to recognize a &lt;switch statement&gt;.
     * <pre>  &lt;switch statement&gt; ::= "switch" "{" &lt;eol&gt; 
     *                                        { "case" &lt;expression&gt; &lt;eol&gt; 
     *                                        "{" &lt;command &gt "}" "}"
     *                                        "}" &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if a  keyword "switch" is present but not 
     * followed by an open braces "{" and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if an switch statement is recognized.
     */
    public boolean isSwitchStatement(){
    	if(!keyword("switch")) return false;
    	if(!symbol("{")) error(" Missing open braces '{'");
    	if(!isEol()) error("");
    	while(keyword("case")){
    		if(!isExpression()) error("Incomplete case action, expression expected");
    		if(!isEol()) error("Syntax error, end of line not found ");
    		while(true){
        		if(isCommand()) continue;
        		else  break;
        	//	else error("Incomplete switch case statement. Missing '}'");
        	}	
    	}
    	if(!symbol("}")) error("Missing close braces '}'");
    	if(!isEol()) error("Syntax error, end of line not found ");
    	return true;
    }
    /**
     * Tries to recognize a &lt;return statement&gt;.
     * <pre>  &lt;return statement&gt; ::= "return" &lt;expression&gt; &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if a keyword return is present but not 
     * followed by an expression and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if an return statement is recognized.
     */
    public boolean isReturnStatement(){
    	if(!keyword("return")) return false;
    	if(!isExpression()) error("Incomplete return statement");
    	if(!isEol()) error("Syntax error, end of line not found");
    	return true;
    }
    /**
     * Tries to recognize a &lt;do statement&gt;.
     * <pre>  &lt;do statement&gt; ::= "do" &lt;variable&gt; [&lt;parameter list&gt;] &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if a  keyword do is present not 
     * followed by a variable and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if an do statement is recognized.
     */
    public boolean isDoStatement(){
    	if(!keyword("do")) return false;
    	if(!isVariable()) error("syntax error, expected variable");
    	if(isParameterList()){
    		if(!isEol()) error("Syntax error, end of line not found");
    	}
    	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    	else{
    	if(!isEol()) error("Syntax error, end of line not found");
    	}
    	return true;
    	
    }
    /**
     * Tries to recognize a &lt;color statement&gt;.
     * <pre>  &lt;color statement&gt; ::= "color" &lt;KEYWORD&gt;  &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if a  keyword color is present not 
     * followed by a color name and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if a color statement is recognized.
     */
    public boolean isColorStatement(){
    	if(!keyword("color")) return false;
    	if(!keyword()) error("missing color name");
    	if(!isEol()) error("Syntax error, end of line not found");
    	return true;
    }
    
    /**
     * Tries to recognize a &lt;block&gt;.
     * <pre>  &lt;color statement&gt; ::= "{" &lt;eol&gt; { &lt;command&gt; } "}"  &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if an open braces "{" is present not 
     * followed by  non-terminals/terminals of the definition.
     * @return <code>true</code> if a block is recognized.
     */
    public boolean isBlock(){
    	if(!symbol("{"))   return false;
    	if(!isEol()) error("Syntak error");
    	while(true){
    		if(isCommand()) continue;
    		else if (symbol("}")) break;
    		else error("Incomplete block. Missing '}'");
    	}
    	if(!isEol()) error("Syntax error, end of line not found");
    	return true;
    }
    /**
     * Tries to recognize a &lt;function definition&gt;.
     * <pre>  &lt;function definition&gt; ::= "define" &lt;NAME&gt; [ "using" &lt;variable &gt; { "," &lt;variable &gt; }] &lt;block&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if a  keyword define is present not 
     * followed by a  name and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if a function definition is recognized.
     */ 
    public boolean isFunctionDefinition(){
    	if(!keyword("define"))   return false;
    	if(!name())   return false;// false or error?
    	if(keyword("using")) {
    		if(!isVariable()) return false;
    		  while (symbol(",")) {
                  if (!isVariable()) error("No variable after ','");
              }
    	}
    	if(!isBlock()) return false;

		return true;
    }
    /**
     * Tries to recognize a &lt;function call&gt;.
     * <pre>  &lt;function call&gt; ::=  &lt;NAME&gt;  &lt;parameter list&gt; </pre>
     * @return <code>true</code> if a function call is recognized.
     */
    public boolean isFunctionCall(){
    	if(!name()) return false;
    	if(!isParameterList()) return false;
    	return true;
    }
    /**
     * Tries to recognize a &lt;eol&gt;.
     * <pre>  &lt;eol&gt; ::=  &lt;eol&gt; { &lt;eol&gt; } </pre> 
     * @return <code>true</code> if an eol is recognized.
     */
    public boolean isEol(){
    	if(!eol())    	return false;
    	while(true){
    		if(eol()) continue;
    		else
    			break;
    	}
    	return true;
    }
//----- Private "helper" methods

    /**
   * Tests whether the next token is a number. If it is, the token
   * is consumed, otherwise it is not.
   *
   * @return <code>true</code> if the next token is a number.
   */
      private boolean number() {
        return nextTokenMatches(Token.Type.NUMBER);
    }

    /**
     * Tests whether the next token is a name. If it is, the token
     * is consumed, otherwise it is not.
     *
     * @return <code>true</code> if the next token is a name.
     */
    private boolean name() {
        return nextTokenMatches(Token.Type.NAME);
    }
    /**
     * Tests whether the next token is a eol. If it is, the token
     * is consumed, otherwise it is not.
     *
     * @return <code>true</code> if the next token is a eol.
     */
    private boolean eol() {
        return nextTokenMatches(Token.Type.EOL);
    }
    /**
     * Tests whether the next token is a eof. If it is, the token
     * is consumed, otherwise it is not.
     *
     * @return <code>true</code> if the next token is a eof.
     */
    private boolean eof() {
        return nextTokenMatches(Token.Type.EOF);
    }
    /**
     * Tests whether the next token is a keyword. If it is, the token
     * is consumed, otherwise it is not.
     *
     * @return <code>true</code> if the next token is a keyword.
     */
    private boolean keyword() {
        return nextTokenMatches(Token.Type.KEYWORD);
    }
  
    /**
     * Tests whether the next token is the expected name. If it is, the token
     * is consumed, otherwise it is not.
     *
     * @param expectedName The String value of the expected next token.
     * @return <code>true</code> if the next token is a name with the expected value.
     */
    private boolean name(String expectedName) {
        return nextTokenMatches(Token.Type.NAME, expectedName);
    }

    /**
     * Tests whether the next token is the expected keyword. If it is, the token
     * is moved to the stack, otherwise it is not.
     *
     * @param expectedKeyword The String value of the expected next token.
     * @return <code>true</code> if the next token is a keyword with the expected value.
     */
    private boolean keyword(String expectedKeyword) {
        return nextTokenMatches(Token.Type.KEYWORD, expectedKeyword);
    }

    /**
     * Tests whether the next token is the expected symbol. If it is,
     * the token is consumed, otherwise it is not.
     *
     * @param expectedSymbol The String value of the token we expect
     *    to encounter next.
     * @return <code>true</code> if the next token is the expected symbol.
     */
    boolean symbol(String expectedSymbol) {
        return nextTokenMatches(Token.Type.SYMBOL, expectedSymbol);
    }

    /**
     * Tests whether the next token has the expected type. If it does,
     * the token is consumed, otherwise it is not. This method would
     * normally be used only when the token's value is not relevant.
     *
     * @param type The expected type of the next token.
     * @return <code>true</code> if the next token has the expected type.
     */
    boolean nextTokenMatches(Token.Type type) {
        Token t = nextToken();
        if (t.type == type) return true;
        pushBack();
        return false;
    }

    /**
     * Tests whether the next token has the expected type and value.
     * If it does, the token is consumed, otherwise it is not. This
     * method would normally be used when the token's value is
     * important.
     *
     * @param type The expected type of the next token.
     * @param value The expected value of the next token; must
     *              not be <code>null</code>.
     * @return <code>true</code> if the next token has the expected type.
     */
    boolean nextTokenMatches(Token.Type type, String value) {
        Token t = nextToken();
        if (type == t.type && value.equals(t.value)) return true;
        pushBack();
        return false;
    }

    /**
     * Returns the next Token.
     * @return The next Token.
     */
    Token nextToken() {
        int code;
        try { code = tokenizer.nextToken(); }
        catch (IOException e) { throw new Error(e); } // Should never happen
        switch (code) {
            case StreamTokenizer.TT_WORD:
                if (Token.KEYWORDS.contains(tokenizer.sval)) {
                    return new Token(Token.Type.KEYWORD, tokenizer.sval);
                }
                return new Token(Token.Type.NAME, tokenizer.sval);
            case StreamTokenizer.TT_NUMBER:
                return new Token(Token.Type.NUMBER, tokenizer.nval + "");
            case StreamTokenizer.TT_EOL:
                return new Token(Token.Type.EOL, "\n");
            case StreamTokenizer.TT_EOF:
                return new Token(Token.Type.EOF, "EOF");
            default:
                return new Token(Token.Type.SYMBOL, ((char) code) + "");
        }
    }

    /**
     * Returns the most recent Token to the tokenizer.
     */
    void pushBack() {
        tokenizer.pushBack();
    }

    /**
     * Utility routine to throw a <code>SyntaxException</code> with the
     * given message.
     * @param message The text to put in the <code>SyntaxException</code>.
     */
    private void error(String message) {
        throw new SyntaxException("Line " + lineNumber + ": " + message);
    }
}
