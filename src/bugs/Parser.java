package bugs;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.*;

import tree.Tree;

/**
 * Parser for numeric expressions. Used as starter code for
 * the Bugs language parser in CIT594, Spring 2015.
 * 
 * @author Dave Matuszek
 * @version February 2015
 */
public class Parser {
    /** The tokenizer used by this Parser. */
    StreamTokenizer tokenizer = null;
    /** The number of the line of source code currently being processed. */
    private int lineNumber = 1;

    /**
     * The stack used for holding Trees as they are created.
     */
    public Stack<Tree<Token>> stack = new Stack<>();

    /**
     * Constructs a Parser for the given string.
     * @param text The string to be parsed.
     */
    public Parser(String text) {
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
     * Tries to build an &lt;expression&gt; on the global stack.
     * <pre>&lt;expression&gt; ::= &lt;arithmetic expression&gt; {  &lt;comparator&gt; &lt;arithmetic expression&gt; }
</pre>
     * A <code>SyntaxException</code> will be thrown if the add_operator
     * is present but not followed by a valid &lt;expression&gt;.
     * @return <code>true</code> if an expression is parsed.
     */
    public boolean isExpression() {
        if (!isArithmeticExpression()) return false;
        while (isComparator()) {
            if (!isArithmeticExpression()) error("Illegal expression after comparator");
            makeTree(2, 3, 1);
        }
        return true;
    }

    /**
     * Tries to build an &lt;expression&gt; on the global stack.
     * <pre>&lt;expression&gt; ::= &lt;term&gt; { &lt;add_operator&gt; &lt;expression&gt; }</pre>
     * A <code>SyntaxException</code> will be thrown if the add_operator
     * is present but not followed by a valid &lt;expression&gt;.
     * @return <code>true</code> if an expression is recognized.
     */
    public boolean isArithmeticExpression() {
        if (!isTerm())
            return false;
        while (isAddOperator()) {
            if (!isTerm()) error("Error in expression after '+' or '-'");
            makeTree(2, 3, 1);
        }
        return true;
    }

    /**
     * Tries to build a &lt;term&gt; on the global stack.
     * <pre>&lt;term&gt; ::= &lt;factor&gt; { &lt;multiply_operator&gt; &lt;term&gt; }</pre>
     * A <code>SyntaxException</code> will be thrown if the multiply_operator
     * is present but not followed by a valid &lt;term&gt;.
     * @return <code>true</code> if a term is parsed.
     */

    public boolean isTerm() {
        if (!isFactor()) {
            return false;
        }
        while (isMultiplyOperator()) {
            if (!isFactor()) {
                error("No term after '*' or '/'");
            }
            makeTree(2, 3, 1);
        }
        return true;
    }

    /**
     * Tries to build a &lt;factor&gt; on the global stack.
     * <pre>&lt;factor&gt; ::= [ &lt;unsigned factor&gt; ] &lt;name&gt;</pre>
     * A <code>SyntaxException</code> will be thrown if the opening
     * parenthesis is present but not followed by a valid
     * &lt;expression&gt; and a closing parenthesis.
     * @return <code>true</code> if a factor is parsed.
     */
    public boolean isFactor() {
        if(symbol("+") || symbol("-")) {
            if (isUnsignedFactor()) {
                makeTree(2, 1);
                return true;
            }
            error("No factor following unary plus or minus");
            return false; // Can't ever get here
        }
        return isUnsignedFactor();
    }

    /**
     * Tries to build an &lt;unsigned factor&gt; on the global stack.
     * <pre>&lt;unsigned factor&gt; ::= &lt;variable&gt; . &lt;variable&gt;
     *                    | &lt;function call&gt;
     *                    | &lt;variable&gt;
     *                    | &lt;number&gt;
     *                    | "(" &lt;expression&gt; ")"</pre>
     * A <code>SyntaxException</code> will be thrown if the opening
     * parenthesis is present but not followed by a valid
     * &lt;expression&gt; and a closing parenthesis.
     * @return <code>true</code> if a factor is parsed.
     */
    public boolean isUnsignedFactor() {
        if (name()) {
            if (symbol(".")) {
                // reference to another Bug
                if (name()) {
                    makeTree(2, 3, 1);
                }
                else error("Incorrect use of dot notation");
            }
            else if (isParameterList()) {
                // function call
                pushNewNode("call");
                makeTree(1, 3, 2);
            }
            else {
                // just a variable; leave it on the stack
            }
        }
        else if (number()) {
            // leave the number on the stack
        }
        else if (symbol("(")) {
            stack.pop();
            if (!isExpression()) {
                error("Error in parenthesized expression");
            }
            if (!symbol(")")) {
                error("Unclosed parenthetical expression");
            }
            stack.pop();
        }
        else {
            return false;
        }
       return true;
    }
    
    /**
     * Tries to build a &lt;parameter list&gt; on the global stack
     * <pre>&ltparameter list&gt; ::= "(" [ &lt;expression&gt; { "," &lt;expression&gt; } ] ")"
     * @return <code>true</code> if a parameter list is parsed.
     */
    public boolean isParameterList() {
        if (!symbol("(")) return false;
        stack.pop(); // remove open paren
        pushNewNode("var");
        if (isExpression()) {
            makeTree(2, 1);
            while (symbol(",")) {
                stack.pop(); // remove comma
                if (!isExpression()) error("No expression after ','");
                makeTree(2, 1);
            }
        }
        if (!symbol(")")) error("Parameter list doesn't end with ')'");
        stack.pop(); // remove close paren
        return true;
    }

    /**
     * Tries to recognize an &lt;add_operator&gt; and put it on the global stack.
     * <pre>&lt;add_operator&gt; ::= "+" | "-"</pre>
     * @return <code>true</code> if an addop is recognized.
     */
    public boolean isAddOperator() {
        return symbol("+") || symbol("-");
    }

    /**
     * Tries to recognize a &lt;multiply_operator&gt; and put it on the global stack.
     * <pre>&lt;multiply_operator&gt; ::= "*" | "/"</pre>
     * @return <code>true</code> if a multiply_operator is recognized.
     */
    public boolean isMultiplyOperator() {
        return symbol("*") || symbol("/");
    }
    
    /**
     * Tries to parse a &lt;variable&gt;; same as &lt;isName&gt;.
     * <pre>&lt;variable&gt; ::= &lt;NAME&gt;</pre>
     * @return <code>true</code> if a variable is parsed.
     */
    public boolean isVariable() {
        return name();
    }
    /**
     * Tries to build a &lt;comparator&gt; on the global stack
     * <pre>&lt;comparator&gt; ::= "<" | "<="| "="| "!="| ">="| ">" </pre>
     * @return <code>true</code> if a comparator is parsed.
     */
    public boolean isComparator() {
    	if(symbol("<")||symbol(">")||symbol("!")){
    		
    		if(symbol("=")) {
    			
    			stack.pop();
    			Tree<Token> s = stack.pop();
    			Token tok = s.getValue();
    			String str = tok.toStringHelper();
    			String ss  = (str).concat("=");
    			pushNewNode(ss);
    			return true;
    		}
    		else return true;
    	}
    	else if(symbol("=")){
    		return true;
    	}
    	return false;
    		
	//	return symbol("<")||symbol(">")||symbol(">=")||symbol("<=")||symbol("=")||symbol("!=");
    	
    }
    /**
     * Tries to build a &lt;program&gt; on the global stack
     * <pre>&lt;program&gt; ::= [ &lt; allbugs code&gt;] &lt;bugs definition&gt;{ &lt;bugs definition&gt;}</pre>
     * A <code>SyntaxException</code> will be thrown if an allbugs code is present not followed by  a &lt;bugs definition&gt;
     * @return <code>true</code> if a program is parsed.
     */
    public boolean isProgram(){
    	//pushNewNode("Allbugs");
    	if(isAllbugsCode()){
    		//stack.pop();
    		if(!isBugDefinition()) error("Bugs Definition not found");
    		pushNewNode("list");
    		makeTree(1,2);
    	}
    	else {
    	pushNewNode("Allbugs");
    	if(!isBugDefinition()) 
    		return false;
    	
    	pushNewNode("list");
    	makeTree(1,2);
    	
    	}
    	while(true){
    		if(isBugDefinition()) {
    			makeTree(2,1);
    			continue;
    		}
    		
    		else  break;
    	}
    	if(!eof()) error("Early terminaton of the program, end of program not recognized");
    	stack.pop();
    	pushNewNode("program");
    	makeTree(1,3,2);
    	return true;
    }
    /**
     * Tries to build an &lt;allbugs code&gt; on the global stack
     * <pre> &lt;allbugs code&gt; ::= "Allbugs" "{" &lt;eol&gt;
     *                                    { &lt;var declaration&gt;}
     *                                    { &lt;function definition&gt;} 
     *                                    "}" &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "Allbugs"  is present not 
     * followed by an open braces and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if an allbugs code is parsed.
     */
    
    public boolean isAllbugsCode(){
    	if(!keyword("Allbugs")) return false;
 //  	stack.pop();
 //   	pushNewNode("Allbugs");
//    	makeTree(1,3,2);
    	if(!symbol("{")) error(" Missing open braces '{'");
    	stack.pop();
    	if(!isEol()) error("Syntax error, end of line not found");
    	stack.pop();
    	pushNewNode("list");
    	while(true){
    		if(isVarDeclaration()){
    			
    			makeTree(2,1);
    			continue;
    		}
    		else  break;
    	}
    	pushNewNode("list");
    	while(true){
    			if(isFunctionDefinition()) {
    			makeTree(2,1);
    			continue;
    		}
    		else  break;
    	}
    	if(!symbol("}")) error("Missing closed braces '}'");
    	stack.pop();
    	if(!isEol()) error("Syntax error, end of line not found");
    	stack.pop();
    	//pushNewNode("Allbugs");
    	makeTree(3,2,1);
    	return true;
    }
    /**
     * Tries to build a &lt;bugs definition&gt; on the global stack
     * <pre> &lt;bugs definition&gt; ::= "Bug" &lt;name&gt; "{" &lt;eol&gt;
     *                                       { &lt;var declaration&gt;}
     *                                       [{ &lt;initialization block&gt;}]
     *                                       { &lt;command&gt;}
     *                                       [{ &lt;command&gt;}]
     *                                       { &lt;function definition&gt;} 
     *                                       "}" &lt;eol&gt;</pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "Bug"  is present not 
     * followed by a name, open braces and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if a bug definition is parsed.
     */
    public boolean isBugDefinition(){
    	if(!keyword("Bug")) return false;
//      stack.pop();
//    	pushNewNode("Bug");
    	if(!name()) error("syntax error :  no bug name found");
    	if(!symbol("{")) error(" Missing open braces '{'");
    	stack.pop();
    	if(!isEol()) error("Syntax error, end of line not found");
    	stack.pop();
    	makeTree(2,1);
    	pushNewNode("list");
    	while(true){
    		if(isVarDeclaration()) {
    		//	pushNewNode("list");
    			makeTree(2,1);
    			continue;
    		}
    		else  break;
    	}
    	makeTree(2,1);
    	
    	if(isInitializationBlock()){
    		//stack.pop();
    		makeTree(2,1);
    		if(!isCommand()) error("command not found");
    		pushNewNode("block");
    		makeTree(1,2);
    	}
    	else{
    	pushNewNode("initially");
        makeTree(2,1);
    	if(!isCommand()) error("command not found");
    	pushNewNode("block");
		makeTree(1,2);
    	}
    	while(true){
    		if(isCommand()) {
    			makeTree(2,1);
    			continue;
    		}
    		
    		else  break;
    	}
    	makeTree(2,1);
    	pushNewNode("list");
    	while(true){
    		if(isFunctionDefinition()) {
    			//pushNewNode("list");
    			makeTree(2,1);
    			continue;
    		}
    		else  break;
    	}
    	if(!symbol("}")) error(" Missing close braces '{'");
    	stack.pop();
    	if(!isEol()) error("Syntax error, end of line not found");
    	stack.pop();
    	makeTree(2,1);
    	return true;
    }

    /**
     * Tries to build a &lt;var declaration&gt; on the global stack
     * <pre> &lt;var declaration&gt; ::= "var" &lt;name&gt; {"," &lt;name&gt;}  &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "var"  is present not 
     * followed by a name and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if a var definition is parsed.
     */
    public boolean isVarDeclaration(){
    	if(!keyword("var")) return false;
    	if(!name()) error("syntax error :  no variable name found");
    	makeTree(2,1);
    	while(symbol(",")){
    		stack.pop();
    		if(!name()) error("Syntax error : no name after ','");
    		makeTree(2,1);
    	}
    	if(!isEol()) error("Syntax error, end of line not found");
    	stack.pop();
    	return true;
    }
    
    /**
     * Tries to build a &lt;initialization block&gt; on the global stack
     * <pre> &lt;initialization block&gt; ::= "initially" &lt;block&gt;  </pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "initially"  is present not 
     * followed by a &lt;block&gt; 
     * @return <code>true</code> if a initialization block is parsed.
     */
    public boolean isInitializationBlock(){
    	if(!keyword("initially")) return false;
    	if(!isBlock()) error("syntax error :  no initializaton found");
    	makeTree(2,1);
    	return true;
    }
    /**
     * Tries to build a &lt;command&gt; on the global stack
     * <pre> &lt;command&gt; ::=  &lt;action&gt;
     *                            | &lt;statement &gt; </pre>
     * @return <code>true</code> if a initialization block is parsed.
     */
    
    public boolean isCommand(){
    	if(!(isAction()||isStatement())) return false;
    	return true;
    	
    }
    
    /**
     * Tries to build a &lt;statement&gt; on the global stack
     * <pre> &lt;statement&gt; ::=  &lt;assignment statement&gt;
     *                           | &lt;loop statement&gt; 
     *                           | &lt;exit if statement&gt;
     *                           | &lt;switch statement&gt;
     *                           | &lt;return statement&gt;
     *                           | &lt;do statement&gt;
     *                           | &lt;color statement&gt; </pre>
     * @return <code>true</code> if a ststement is parsed.
     */
    public boolean isStatement(){
    	if(!(isAssignmentStatement()||isLoopStatement()||isExitIfStatement()||isSwitchStatement()||isReturnStatement()||isDoStatement()||isColorStatement())) return false;
    	return true;
    }
    /**
     * Tries to build a &lt;action&gt; on the global stack.
     * <pre> &lt;action&gt; ::=  &lt;move action &gt;
     *                        | &lt;moveto action &gt; 
     *                        | &lt;turn action &gt;
     *                        | &lt;turnto action &gt;
     *                        | &lt;line action &gt; </pre>
     * @return <code>true</code> if an action is parsed.
     */
    public boolean isAction(){
    	if(!(isMoveAction()||isMoveToAction()||isTurnAction()||isTurnToAction()||isLineAction())) return false;
    	return true;
    }
    /**
     * Tries to build a &lt;move action&gt; on the global stack
     * <pre>  &lt;move action&gt; ::= "move" &lt;expression&gt;  &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "move"  is present not 
     * followed by an expression and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if a move action is parsed.
     */
    
    public boolean isMoveAction(){
    	if(!keyword("move")) return false;
    	if(!isExpression()) error("Incomplete move action");
    	makeTree(2,1);
    	if(!isEol()) error("Syntax error, end of line not found");
    	stack.pop();
    	return true;
    }
    /**
     * Tries to build a &lt;moveto action&gt; on the global stack
     * <pre>  &lt;moveto action&gt; ::= "moveto" &lt;expression&gt; "," &lt;expression&gt; &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "moveto"  is present not 
     * followed by an expression and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if a moveto action is parsed.
     */
    public boolean isMoveToAction(){
     	if(!keyword("moveto")) return false;
    	if(!isExpression()) error("Incomplete move action");
    	if(!symbol(",")) error("syntax error , expected ','");
    	stack.pop();
    	if(!isExpression()) error("Incomplete move to action");
    	makeTree(3,2,1);
    	if(!isEol()) error("Syntax error, end of line not found ");
    	stack.pop();
    	return true;
    }
    /**
     * Tries to build a &lt;turn action&gt; on the global stack.
     * <pre>  &lt;turn action&gt; ::= "turn" &lt;expression&gt;  &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "turn"  is present not 
     * followed by an expression and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if a turn action is parsed.
     */
    public boolean isTurnAction(){
    	if(!keyword("turn")) return false;
    	if(!isExpression()) error("Incomplete move action");
    	makeTree(2,1);
    	if(!isEol()) error("Syntax error, end of line not found");
    	stack.pop();
    	return true;
    }
    /**
     * Tries to build a &lt;turnto action&gt; on the global stack.
     * <pre>  &lt;turnto action&gt; ::= "turnto" &lt;expression&gt;  &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "turnto"  is present not 
     * followed by an expression and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if a turnto action is parsed.
     */
    public boolean isTurnToAction(){
    	if(!keyword("turnto")) return false;
    	if(!isExpression()) error("Incomplete move action");
      	makeTree(2,1);
    	if(!isEol()) error("Syntax error, end of line not found");
    	stack.pop();
    	return true;
    }
    /**
     * Tries to build a &lt;line action&gt; on the global stack.
     * <pre>  &lt;line action&gt; ::= "line" &lt;expression&gt; "," &lt;expression&gt; "," &lt; expression&gt; "," &lt;expression&gt; &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "line"  is present not 
     * followed by an expression and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if a line action is parsed.
     */
    public boolean isLineAction(){
    	if(!keyword("line")) return false;
    	if(!isExpression()) error("Expression not found");
    	makeTree(2,1);
    	if(!symbol(",")) error("syntax error , expected ','");
    	stack.pop();
    	if(!isExpression()) error("Expression not found");
    	makeTree(2,1);
    	if(!symbol(",")) error("syntax error , expected ','");
    	stack.pop();
    	if(!isExpression()) error("Expression not found");
    	makeTree(2,1);
    	if(!symbol(",")) error("syntax error , expected ','");
    	stack.pop();
    	if(!isExpression()) error("Expression not found");
    	makeTree(2,1);
    	if(!isEol()) error("Syntax error, end of line not found ");
    	stack.pop();
    	return true;
    }
    /**
     * Tries to build a &lt;assignment statement&gt; on the global stack
     * <pre>  &lt;assignment statement&gt; ::= &lt;variable&gt; "=" expression&gt; &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if a vraiable  is present not 
     * followed by a "=" and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if an assignment statement is parsed.
     */
    public boolean isAssignmentStatement(){
    	if(!isVariable())return false;
    	pushNewNode("assign");
    	makeTree(1,2);
    	if(!symbol("=")) error("Incomplete assignment statement, expected an expression");
     	stack.pop();
    	if(!isExpression()) error("expression not found after assignment");
    	makeTree(2,1);
    	if(!isEol()) error("Syntax error, end of line not found");
    	stack.pop();
    	return true;
    }
    /**
     * Tries to build a &lt;loop statement&gt; on the global stack.
     * <pre>  &lt;loop statement&gt; ::= "loop" &lt;block&gt;   </pre>
     * A <code>SyntaxException</code> will be thrown if the keyword "loop"  is present not 
     * followed by a block.
     * @return <code>true</code> if a loop statement action is parsed.
     */
    public boolean isLoopStatement(){
    	if(!keyword("loop")) return false;
    	if(!isBlock()) error("Incomplete loop");
    	makeTree(2,1);
    	return true;
    }
    /**
     * Tries to recognize a &lt;exit if statement&gt; on the global stack.
     * <pre>  &lt;exit if statement&gt; ::= "exit" "if" &lt;expression&gt; &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if a  keyword "exit" is present but not 
     * followed by a keyword "if" and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if an exit if statement is parsed.
     */
    public boolean isExitIfStatement(){
    	if(!keyword("exit")) return false;
    	if(!keyword("if")) error("Syntax error");
    	stack.pop();
    	if(!isExpression()) error("Expression not found after exitif ");
    	makeTree(2,1);
    	if(!isEol()) error("Syntax error, end of line not found");
    	stack.pop();
    	return true;
    }
    /**
     * Tries to recognize a &lt;switch statement&gt; on the global stack.
     * <pre>  &lt;switch statement&gt; ::= "switch" "{" &lt;eol&gt; 
     *                                        { "case" &lt;expression&gt; &lt;eol&gt; 
     *                                        "{" &lt;command &gt "}" "}"
     *                                        "}" &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if a  keyword "switch" is present but not 
     * followed by an open braces "{" and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if an switch statement is parsed.
     */
    public boolean isSwitchStatement(){
    	if(!keyword("switch")) return false;
    	if(!symbol("{")) error(" Missing open braces '{'");
    	stack.pop();
    	if(!isEol()) error("no end of line found");
    	stack.pop();
    	if(!keyword("case")){
    		if(!symbol("}")) error("Missing close braces '}'");
        	stack.pop();
        	if(!isEol()) error("Syntax error, end of line not found ");
        	stack.pop();
        	return true;
    	}
    	pushBack();
    	stack.pop();
    	while(keyword("case")){
    		//stack.pop();
    		if(!isExpression()) error("Incomplete case action, expression expected");
    		makeTree(2,1);
    		if(!isEol()) error("Syntax error, end of line not found ");
    		stack.pop();
    		pushNewNode("block");
    		while(true){
        		if(isCommand()){
        			makeTree(2,1);
        			continue;
        		}
        		else  break;
        	//	else error("Incomplete switch case statement. Missing '}'");
        	}	
    		makeTree(2,1);
    	}
    	makeTree(2,1);
    	if(!symbol("}")) error("Missing close braces '}'");
    	stack.pop();
    	if(!isEol()) error("Syntax error, end of line not found ");
    	stack.pop();
    	return true;
    }
    /**
     * Tries to recognize a &lt;return statement&gt; on the global stack.
     * <pre>  &lt;return statement&gt; ::= "return" &lt;expression&gt; &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if a keyword return is present but not 
     * followed by an expression and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if an return statement is parsed.
     */
    public boolean isReturnStatement(){
    	if(!keyword("return")) return false;
    	if(!isExpression()) error("Incomplete return statement");
    	makeTree(2,1);
    	if(!isEol()) error("Syntax error, end of line not found");
    	stack.pop();
    	return true;
    }
    /**
     * Tries to recognize a &lt;do statement&gt; on the global stack.
     * <pre>  &lt;do statement&gt; ::= "do" &lt;variable&gt; [&lt;parameter list&gt;] &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if a  keyword do is present not 
     * followed by a variable and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if an do statement is parsed.
     */
    public boolean isDoStatement(){
    	if(!keyword("do")) return false;
    	stack.pop();
    	pushNewNode("call");
    	if(!isVariable()) error("syntax error, expected variable");
    	makeTree(2,1);
    	if(isParameterList()){
    		makeTree(2,1);
    		if(!isEol()) error("Syntax error, end of line not found");
    		stack.pop();
    	}
    	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    	//%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
    	else{
    	if(!isEol()) error("Syntax error, end of line not found");
    	stack.pop();
    	}
    	return true;
    	
    }
    /**
     * Tries to recognize a &lt;color statement&gt; on the global stack.
     * <pre>  &lt;color statement&gt; ::= "color" &lt;KEYWORD&gt;  &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if a  keyword color is present not 
     * followed by a color name and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if a color statement is parsed.
     */
    public boolean isColorStatement(){
    	if(!keyword("color")) return false;
    	if(!keyword()) error("missing color name");
    	makeTree(2,1);
    	if(!isEol()) error("Syntax error, end of line not found");
    	stack.pop();
    	return true;
    }
    
    /**
     * Tries to recognize a &lt;block&gt; on the global stack.
     * <pre>  &lt;color statement&gt; ::= "{" &lt;eol&gt; { &lt;command&gt; } "}"  &lt;eol&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if an open braces "{" is present not 
     * followed by  non-terminals/terminals of the definition.
     * @return <code>true</code> if a block is parsed.
     */
    public boolean isBlock(){
    	if(!symbol("{"))   return false;
    	stack.pop();
    	pushNewNode("block");
    	if(!isEol()) error("Syntak error");
    	stack.pop();
    	while(true){
    		if(isCommand()) {
    			makeTree(2,1);
    			continue;
    		}
    		else if (symbol("}")) {
    			stack.pop();
    			break;
    		}
    		else error("Incomplete block. Missing '}'");
    	}
    	if(!isEol()) error("Syntax error, end of line not found");
    	stack.pop();
    	return true;
    }
    /**
     * Tries to build a &lt;function definition&gt; on the global stack.
     * <pre>  &lt;function definition&gt; ::= "define" &lt;NAME&gt; [ "using" &lt;variable &gt; { "," &lt;variable &gt; }] &lt;block&gt; </pre>
     * A <code>SyntaxException</code> will be thrown if a  keyword define is present not 
     * followed by a  name and the following non-terminals/terminals of the definition.
     * @return <code>true</code> if a function definition is parsed.
     */ 
    public boolean isFunctionDefinition(){
    	if(!keyword("define"))   return false;
    	stack.pop();
    	pushNewNode("function");
    	if(!name())   return false;// false or error?
    	makeTree(2,1);
    	pushNewNode("var");
    	if(keyword("using")) {
    		stack.pop();
    		if(!isVariable()) return false;
    		makeTree(2,1);
    		  while (symbol(",")) {
    				stack.pop();
                  if (!isVariable()) error("No variable after ','");
                  makeTree(2,1);
              }
    	}
    	if(!isBlock()) return false;
    	makeTree(3,2,1);
		return true;
    }
    /**
     * Tries to build a &lt;function call&gt; on the global stack.
     * <pre>  &lt;function call&gt; ::=  &lt;NAME&gt;  &lt;parameter list&gt; </pre>
     * @return <code>true</code> if a function call is parsed.
     */
    public boolean isFunctionCall(){
    	
    	if(!name()) return false;
    	pushNewNode("call");
    	makeTree(1,2);
    	if(!isParameterList()) return false;
    	makeTree(2,1);
    	return true;
    }
    /**
     * Tries to build a &lt;eol&gt; on the global stack. (will be popped later)
     * <pre>  &lt;eol&gt; ::=  &lt;eol&gt; { &lt;eol&gt; } </pre> 
     * @return <code>true</code> if an eol is parsed.
     */
    public boolean isEol(){
    	if(!eol()) 	return false;
    	while(true){
    		if(eol()) {
        		stack.pop();
    			continue;
    		}
    		else
    			break;
    	}
    	return true;
    }
    //------------------------- Private "helper" methods
    
    /**
     * Creates a new Tree consisting of a single node containing a
     * Token with the correct type and the given <code>value</code>,
     * and pushes it onto the global stack. 
     *
     * @param value The value of the token to be pushed onto the global stack.
     */
    private void pushNewNode(String value) {
        stack.push(new Tree<>(new Token(Token.typeOf(value), value)));
    }

    /**
     * Tests whether the next token is a number. If it is, the token
     * is moved to the stack, otherwise it is not.
     * 
     * @return <code>true</code> if the next token is a number.
     */
    private boolean number() {
        return nextTokenMatches(Token.Type.NUMBER);
    }

    /**
     * Tests whether the next token is a name. If it is, the token
     * is moved to the stack, otherwise it is not.
     * 
     * @return <code>true</code> if the next token is a name.
     */
    private boolean name() {
        return nextTokenMatches(Token.Type.NAME);
    }

    /**
     * Tests whether the next token is the expected name. If it is, the token
     * is moved to the stack, otherwise it is not.
     * 
     * @param expectedName The String value of the expected next token.
     * @return <code>true</code> if the next token is a name with the expected value.
     */
    private boolean name(String expectedName) {
        return nextTokenMatches(Token.Type.NAME, expectedName);
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
     * Tests whether the next token is a keyword. If it is, the token
     * is consumed, otherwise it is not.
     *
     * @return <code>true</code> if the next token is a keyword.
     */
    private boolean keyword() {
        return nextTokenMatches(Token.Type.KEYWORD);
    }
    /**
     * Tests whether the next token is the expected symbol. If it is,
     * the token is moved to the stack, otherwise it is not.
     * 
     * @param expectedSymbol The single-character String that is expected
     *        as the next symbol.
     * @return <code>true</code> if the next token is the expected symbol.
     */
    private boolean symbol(String expectedSymbol) {
        return nextTokenMatches(Token.Type.SYMBOL, expectedSymbol);
    }

    /**
     * If the next Token has the expected type, it is used as the
     * value of a new (childless) Tree node, and that node
     * is then pushed onto the stack. If the next Token does not
     * have the expected type, this method effectively does nothing.
     * 
     * @param type The expected type of the next token.
     * @return <code>true</code> if the next token has the expected type.
     */
    private boolean nextTokenMatches(Token.Type type) {
        Token t = nextToken();
        if (t.type == type) {
            stack.push(new Tree<>(t));
            return true;
        }
        pushBack();
        return false;
    }

    /**
     * If the next Token has the expected type and value, it is used as
     * the value of a new (childless) Tree node, and that node
     * is then pushed onto the stack; otherwise, this method does
     * nothing.
     * 
     * @param type The expected type of the next token.
     * @param value The expected value of the next token; must
     *              not be <code>null</code>.
     * @return <code>true</code> if the next token has the expected type.
     */
    private boolean nextTokenMatches(Token.Type type, String value) {
        Token t = nextToken();
        if (type == t.type && value.equals(t.value)) {
            stack.push(new Tree<>(t));
            return true;
        }
        pushBack();
        return false;
    }

    /**
     * Returns the next Token. Increments the global variable
     * <code>lineNumber</code> when an EOL is returned.
     * 
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
                lineNumber++;
                return new Token(Token.Type.EOL, "\n");
            case StreamTokenizer.TT_EOF:
                return new Token(Token.Type.EOF, "EOF");
            default:
                return new Token(Token.Type.SYMBOL, ((char) code) + "");
        }
    }

    /**
     * Returns the most recent Token to the tokenizer. Decrements the global
     * variable <code>lineNumber</code> if an EOL is pushed back.
     */
    void pushBack() {
        tokenizer.pushBack();
        if (tokenizer.ttype == StreamTokenizer.TT_EOL) lineNumber--;
    }

    /**
     * Assembles some number of elements from the top of the global stack
     * into a new Tree, and replaces those elements with the new Tree.<p>
     * <b>Caution:</b> The arguments must be consecutive integers 1..N,
     * in any order, but with no gaps; for example, makeTree(2,4,1,5)
     * would cause problems (3 was omitted).
     * 
     * @param rootIndex Which stack element (counting from 1) to use as
     * the root of the new Tree.
     * @param childIndices Which stack elements to use as the children
     * of the root.
     */    
    void makeTree(int rootIndex, int... childIndices) {
        // Get root from stack
        Tree<Token> root = getStackItem(rootIndex);
        // Get other trees from stack and add them as children of root
        for (int i = 0; i < childIndices.length; i++) {
            root.addChild(getStackItem(childIndices[i]));
        }
        // Pop root and all children from stack
        for (int i = 0; i <= childIndices.length; i++) {
            stack.pop();
        }
        // Put the root back on the stack
        stack.push(root);
    }
    
    /**
     * Returns the n-th item from the top of the global stack (counting the
     * top element as 1).
     * 
     * @param n Which stack element to return.
     * @return The n-th element in the global stack.
     */
    private Tree<Token> getStackItem(int n) {
        return stack.get(stack.size() - n);
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

