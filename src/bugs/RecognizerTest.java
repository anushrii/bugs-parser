package bugs;

import static org.junit.Assert.*;

import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;


/**
 * Test class for Bugs recognizer.
 * @author David Matuszek
 */
public class RecognizerTest {
    
    Recognizer r0, r1, r2, r3, r4, r5, r6, r7, r8;
    
    /**
     * Constructor for RecognizerTest.
     */
    public RecognizerTest() {
        r0 = new Recognizer("2 + 2");
        r1 = new Recognizer("");
    }


    @Before
    public void setUp() throws Exception {
        r0 = new Recognizer("");
        r1 = new Recognizer("250");
        r2 = new Recognizer("hello");
        r3 = new Recognizer("(xyz + 3)");
        r4 = new Recognizer("12 * 5 - 3 * 4 / 6 + 8");
        r5 = new Recognizer("12 * ((5 - 3) * 4) / 6 + (8)");
        r6 = new Recognizer("17 +");
        r7 = new Recognizer("22 *");
        r8 = new Recognizer("#");
    }

    @Test
    public void testRecognizer() {
        r0 = new Recognizer("");
        r1 = new Recognizer("2 + 2");
    }

    @Test
    public void testIsArithmeticExpression() {
        assertTrue(r1.isArithmeticExpression());
        assertTrue(r2.isArithmeticExpression());
        assertTrue(r3.isArithmeticExpression());
        assertTrue(r4.isArithmeticExpression());
        assertTrue(r5.isArithmeticExpression());

        assertFalse(r0.isArithmeticExpression());
        assertFalse(r8.isArithmeticExpression());

        try {
            assertFalse(r6.isArithmeticExpression());
            fail();
        }
        catch (SyntaxException e) {
        }
        try {
            assertFalse(r7.isArithmeticExpression());
            fail();
        }
        catch (SyntaxException e) {
        }
    }

    @Test
    public void testIsArithmeticExpressionWithUnaryMinus() {
        assertTrue(new Recognizer("-5").isArithmeticExpression());
        assertTrue(new Recognizer("12+(-5*10)").isArithmeticExpression());
        assertTrue(new Recognizer("+5").isArithmeticExpression());
        assertTrue(new Recognizer("12+(+5*10)").isArithmeticExpression());
    }

    @Test
    public void testIsTerm() {
        assertFalse(r0.isTerm()); // ""
        
        assertTrue(r1.isTerm()); // "250"
        
        assertTrue(r2.isTerm()); // "hello"
        
        assertTrue(r3.isTerm()); // "(xyz + 3)"
        followedBy(r3, "");
        
        assertTrue(r4.isTerm());  // "12 * 5 - 3 * 4 / 6 + 8"
        assertEquals(new Token(Token.Type.SYMBOL, "-"), r4.nextToken());
        assertTrue(r4.isTerm());
        followedBy(r4, "+ 8");

        assertTrue(r5.isTerm());  // "12 * ((5 - 3) * 4) / 6 + (8)"
        assertEquals(new Token(Token.Type.SYMBOL, "+"), r5.nextToken());
        assertTrue(r5.isTerm());
        followedBy(r5, "");
    }

    @Test
    public void testIsFactor() {
        assertTrue(r1.isFactor());
        assertTrue(r2.isFactor());
        assertTrue(r3.isFactor());
        assertTrue(r4.isFactor()); followedBy(r4, "* 5 - 3 * 4 / 6 + 8");
        assertTrue(r5.isFactor()); followedBy(r5, "* ((5");
        assertTrue(r6.isFactor()); followedBy(r6, "+");
        assertTrue(r7.isFactor()); followedBy(r7, "*");

        assertFalse(r0.isFactor());
        assertFalse(r8.isFactor()); followedBy(r8, "#");

        Recognizer r = new Recognizer("foo()");
        assertTrue(r.isFactor());
        r = new Recognizer("bar(5, abc, 2+3)+");
        assertTrue(r.isFactor()); followedBy(r, "+");

        r = new Recognizer("foo.bar$");
        assertTrue(r.isFactor()); followedBy(r, "$");
        
        r = new Recognizer("123.123");
        assertEquals(new Token(Token.Type.NUMBER, "123.123"), r.nextToken());
        
        r = new Recognizer("5");
        assertEquals(new Token(Token.Type.NUMBER, "5.0"), r.nextToken());
    }
    
    @Test
    public void testIsParameterList() {
        Recognizer r = new Recognizer("() $");
        assertTrue(r.isParameterList()); followedBy(r, "$");
        r = new Recognizer("(5) $");
        assertTrue(r.isParameterList()); followedBy(r, "$");
        r = new Recognizer("(bar, x+3) $");
        assertTrue(r.isParameterList()); followedBy(r, "$");
    }

    @Test
    public void testIsAddOperator() {
        Recognizer r = new Recognizer("+ - $");
        assertTrue(r.isAddOperator());
        assertTrue(r.isAddOperator());
        assertFalse(r.isAddOperator());
        followedBy(r, "$");
    }

    @Test
    public void testIsMultiplyOperator() {
        Recognizer r = new Recognizer("* / $");
        assertTrue(r.isMultiplyOperator());
        assertTrue(r.isMultiplyOperator());
        assertFalse(r.isMultiplyOperator());
        followedBy(r, "$");
    }

    @Test
    public void testIsVariable() {
        Recognizer r = new Recognizer("foo 23 bar +");
        assertTrue(r.isVariable());
        
        assertFalse(r.isVariable());
        assertTrue(r.isFactor());
        
        assertTrue(r.isVariable());
        
        assertFalse(r.isVariable());
        assertTrue(r.isAddOperator());
    }

    @Test
    public void testIsComparator() {
        Recognizer r = new Recognizer(" a >= b");
        assertTrue(r.isVariable());
        assertTrue(r.isComparator());
        assertFalse(r.isComparator());
        assertTrue(r.isVariable());
    }

//    <program> ::= [ <allbugs code> ]
//            <bug definition>
//          { <bug definition> } 
    @Test
    public void testIsProgram() {
   	  Recognizer r1 = new Recognizer("Allbugs { \n var beebui, beebuku, beebudu \n define foo using a,b,c { \n } \n } \n" +
   	  		"Bug beebu { \n var beebui, beebuku, beebudu \n initially { \n }\n moveto 8,7 \n } \n ");
      Recognizer r2 = new Recognizer("Bugs bunny { \n move 1 \n } \n \n");
      assertTrue(r1.isProgram());
      assertFalse(r2.isProgram());
      Recognizer r3 = new Recognizer("Bug bunny { \n move 1 \n } \n \n x,y");
      try {
          assertFalse(r3.isProgram());
          fail();
      }
      catch (SyntaxException e) {
      }
    }
	//    
	//    <allbugs code> ::= "Allbugs"  "{" <eol>
	//    { <var declaration> }
	//    { <function definition> }
	//"}" <eol>
    @Test
    public void testIsAllbugsCode() {
    	  Recognizer r1 = new Recognizer("Allbugs { \n var beebui, beebuku, beebudu \n define foo using a,b,c { \n } \n } \n");
          Recognizer r2 = new Recognizer("Bugs bunny { \n move 1 \n } \n \n");
          assertTrue(r1.isAllbugsCode());
          assertFalse(r2.isAllbugsCode());
          Recognizer r3 = new Recognizer("Allbugs { \n var beebui, beebuku, beebudu \n define foo using a,b,c { \n } \n  \n");
          try {
              assertFalse(r3.isAllbugsCode());
              fail();
          }
          catch (SyntaxException e) {
          }
    }
    
    
		//    <bug definition> ::= "Bug" <name> "{" <eol>
		//   	 { <var declaration> }
		//     	 [ <initialization block> ]
		// 	      <command>
		//  	  { <command> }
		//  	  { <function definition> }
		//		  "}" <eol>
    @Test
    public void testIsBugDefinition() {
        Recognizer r1 = new Recognizer("Bug beebu { \n var beebui, beebuku, beebudu \n initially { \n }\n moveto 8,7 \n } \n");
        Recognizer r2 = new Recognizer("Bugs bunny { \n move 1 \n } \n \n");
        assertTrue(r1.isBugDefinition());
        assertFalse(r2.isBugDefinition());
        Recognizer r3 = new Recognizer("Bug beebu { \n var beebui, beebuku, beebudu initially { \n } moveto 8 } \n");
        try {
            assertFalse(r3.isBugDefinition());
            fail();
        }
        catch (SyntaxException e) {
        }
    }
    
    // <var declaration> ::= "var" <NAME> { "," <NAME> } <eol>       
    @Test
    public void testIsVarDeclaration() {
        Recognizer r1 = new Recognizer("var a,b,b \n");
        Recognizer r2 = new Recognizer("variable a,b,b, b + n \n");
        assertTrue(r1.isVarDeclaration());
        assertFalse(r2.isVarDeclaration());
        Recognizer r3 = new Recognizer("var a,b, 8+9 \n");
        try {
            assertFalse(r3.isVarDeclaration());
            fail();
        }
        catch (SyntaxException e) {
        }
    }
    
    // <initialization block> ::= "initially" <block>
    @Test
    public void testIsInitializationBlock() {
        Recognizer r = new Recognizer("initially { \n } \n");
        Recognizer r2 = new Recognizer("init { \n move 1 \n } \n \n");
        assertTrue(r.isInitializationBlock());
        assertFalse(r2.isInitializationBlock());
        Recognizer r3 = new Recognizer("initially \n { a = b } a= b \n \n");
        try {
            assertFalse(r3.isInitializationBlock());
            fail();
        }
        catch (SyntaxException e) {
        }
    }
//    <command> ::= <action>
//    | <statement> 
    @Test
    public void testIsCommand() {
        Recognizer r = new Recognizer("a = 5 + 8 \n");
        Recognizer r1 = new Recognizer("*a =  5 + 8 \n");
        assertTrue(r.isAssignmentStatement());
        assertFalse(r1.isAssignmentStatement());
    	 Recognizer r2 = new Recognizer("move 0 \n");
         Recognizer r3 = new Recognizer("moveto  5 \n");
         assertTrue(r2.isMoveAction());
         assertFalse(r3.isMoveAction());
        
    }
    

//<statement> ::= <assignment statement>
//              | <loop statement>
//              | <exit if statement>
//              | <switch statement>
//              | <return statement>
//              | <do statement>
//              | <color statement>
    @Test
    public void testIsStatement() {
        Recognizer r = new Recognizer("a = 5 + 8 \n");
        Recognizer r1 = new Recognizer("*a =  5 + 8 \n");
        assertTrue(r.isAssignmentStatement());
        assertFalse(r1.isAssignmentStatement());
        Recognizer r2 = new Recognizer("exit if -a \n");
        assertTrue( r2.isExitIfStatement());
        Recognizer r3 = new Recognizer("exits if a=5 \n");
        assertFalse( r3.isExitIfStatement());
        Recognizer r4 = new Recognizer("exit if a+ \n");
        try {
            assertFalse(r4.isExitIfStatement());
            fail();
        }
        catch (SyntaxException e) {
        }
    }
//    //
//    <action> ::= <move action>
			//    | <moveto action>
			//    | <turn action>
			//    | <turnto action>
			//    | <line action>
    @Test
    public void testIsAction() {
    	 Recognizer r = new Recognizer("move 0 \n");
         Recognizer r1 = new Recognizer("moveto  5 \n");
         assertTrue(r.isMoveAction());
         assertFalse(r1.isMoveAction());
         Recognizer r2 = new Recognizer("move  5 + 8, 8 \n");
         try {
             assertFalse(r2.isMoveAction());
             fail();
         }
         catch (SyntaxException e) {
         }
         Recognizer r3 = new Recognizer("moveto 0,8 \n");
         Recognizer r4 = new Recognizer("move to  5, a + b \n");
         assertTrue(r3.isMoveToAction());
         assertFalse(r4.isMoveToAction());
    	 Recognizer r5 = new Recognizer("turnto 0 \n");
         Recognizer r6 = new Recognizer("turn to  5\n");
         assertTrue(r5.isTurnToAction());
         assertFalse(r6.isTurnToAction());
    }
    
    // <move action> ::= "move" <expression> <eol>
    @Test
    public void testIsMoveAction() {
      	 Recognizer r = new Recognizer("move 0 \n");
         Recognizer r1 = new Recognizer("moveto  5 \n");
         assertTrue(r.isMoveAction());
         assertFalse(r1.isMoveAction());
         Recognizer r2 = new Recognizer("move  5 + 8, 8 \n");
         try {
             assertFalse(r2.isMoveAction());
             fail();
         }
         catch (SyntaxException e) {
         }
    }
    
    //<moveto action> ::= "moveto" <expression> "," <expression> <eol>
    @Test
    public void testIsMoveToAction() {
    	 Recognizer r = new Recognizer("moveto 0,8 \n");
         Recognizer r1 = new Recognizer("move to  5, a + b \n");
         assertTrue(r.isMoveToAction());
         assertFalse(r1.isMoveToAction());
         Recognizer r2 = new Recognizer("moveto  5 + 8, a + b, 8\n");
         try {
             assertFalse(r2.isMoveToAction());
             fail();
         }
         catch (SyntaxException e) {
         }
    }
    
    // <turn action> ::= "turn" <expression> <eol>
    @Test
    public void testIsTurnAction() {
   	 Recognizer r = new Recognizer("turn 0 \n");
     Recognizer r1 = new Recognizer("turnto  5 \n");
     assertTrue(r.isTurnAction());
     assertFalse(r1.isTurnAction());
     Recognizer r2 = new Recognizer("turn  5 + 8, 8 \n");
     try {
         assertFalse(r2.isTurnAction());
         fail();
     }
     catch (SyntaxException e) {
     }
    }
    
    // <turnto action> ::= "turnto" <expression> <eol>
    @Test
    public void testIsTurnToAction() {
    	 Recognizer r = new Recognizer("turnto 0 \n");
         Recognizer r1 = new Recognizer("turn to  5\n");
         assertTrue(r.isTurnToAction());
         assertFalse(r1.isTurnToAction());
         Recognizer r2 = new Recognizer("turnto  5 + 8, 8 \n");
         try {
             assertFalse(r2.isTurnToAction());
             fail();
         }
         catch (SyntaxException e) {
         }
    }
   
    
    // <line action> ::= "line" <expression> ","<expression> ","<expression> "," <expression> <eol>
    @Test
    public void testIsLineAction() {
        Recognizer r = new Recognizer("line 5 + 8, 7, 9 ,0 \n");
        Recognizer r1 = new Recognizer("lines  5 + 8,8 \n");
        assertTrue(r.isLineAction());
        assertFalse(r1.isLineAction());
        Recognizer r2 = new Recognizer("line  5 + 8, * \n");
        try {
            assertFalse(r2.isLineAction());
            fail();
        }
        catch (SyntaxException e) {
        }
    }
    
    //<assignment statement> ::= <variable> "=" <expression> <eol>
    @Test
    public void testIsAssignmentStatement() {
        Recognizer r = new Recognizer("a = 5 + 8 \n");
        Recognizer r1 = new Recognizer("*a =  5 + 8 \n");
        assertTrue(r.isAssignmentStatement());
        assertFalse(r1.isAssignmentStatement());
        Recognizer r2 = new Recognizer("a =  5 + 8* \n");
        try {
            assertFalse(r2.isAssignmentStatement());
            fail();
        }
        catch (SyntaxException e) {
        }
    }
    
    //<loop statement> ::= "loop" <block>
    @Test
    public void testIsLoopStatement() {
        Recognizer r = new Recognizer("loop { \n } \n");
        Recognizer r2 = new Recognizer("loop { \n move 1 \n } \n");
        assertTrue(r.isLoopStatement());
        assertTrue(r2.isLoopStatement());
        Recognizer r3 = new Recognizer("loop if a=5+ \n");
        try {
            assertFalse(r3.isLoopStatement());
            fail();
        }
        catch (SyntaxException e) {
        }
    }
   
    //<exit if statement> ::= "exit" "if" <expression> <eol>

    @Test
    public void testIsExitIfStatement() {
        Recognizer r = new Recognizer("exit if a=5 \n");
        assertTrue( r.isExitIfStatement());
        Recognizer r1 = new Recognizer("exits if a=5 \n");
        assertFalse( r1.isExitIfStatement());
        Recognizer r3 = new Recognizer("exit if a=5+ \n");
        try {
            assertFalse(r3.isExitIfStatement());
            fail();
        }
        catch (SyntaxException e) {
        }
    }
    
//    <switch statement> ::= "switch" "{" <eol>
//         { "case" <expression> <eol>
//         { <command> } }
//         "}" <eol>
    @Test
    public void testIsSwitchStatement() {
        Recognizer r = new Recognizer("switch { \n  case a \n move 1 \n  } \n");
        assertTrue( r.isSwitchStatement());
        Recognizer r1 = new Recognizer("switch { \n } \n");
        assertTrue( r1.isSwitchStatement());
        Recognizer r2 = new Recognizer("switch { } \n");
        try {
            assertFalse(r2.isSwitchStatement());
            fail();
        }
        catch (SyntaxException e) {
        }
    }
    
    //<return statement> ::= "return" <expression> <eol>
    @Test
    public void testIsReturnStatement() {
        Recognizer r = new Recognizer("return null \n");
        Recognizer r1 = new Recognizer("returns null \n");
        Recognizer r2 = new Recognizer("return null");
        assertTrue( r.isReturnStatement());
        assertFalse(r1.isReturnStatement());
        try {
            assertFalse(r2.isReturnStatement());
            fail();
        }
        catch (SyntaxException e) {
        }
    }
    
    //<do statement> ::= "do" <variable> [ <parameter list> ] <eol>
    @Test
    public void testIsDoStatement() {
        Recognizer r = new Recognizer("do a (a,2,3) \n");
        Recognizer r1 = new Recognizer("(a,2,3)");
        assertTrue( r1.isParameterList());
        assertTrue( r.isDoStatement());
        assertFalse(r.isColorStatement());
    }
    
    //<color statement> ::= "color" <KEYWORD> <eol>
    @Test
    public void testIsColorStatement() {
        Recognizer r = new Recognizer("color cyan \n ");
        Recognizer r1 = new Recognizer("Newcolor burgandy \n ");
        Recognizer r2 = new Recognizer("color burgandy \n ");
        assertTrue( r.isColorStatement());
        assertFalse(r1.isColorStatement());
        try {
            assertFalse(r2.isColorStatement());
            fail();
        }
        catch (SyntaxException e) {
        }
    }
    
    //<block> ::= "{" <eol> { <command> }  "}" <eol>
    //<command> ::= <action>
    //		     | <statement>
    @Test
    public void testIsBlock() {
        Recognizer r = new Recognizer("{ \n move 1 \n } \n");
        Recognizer r1 = new Recognizer("move 1 \n");
        Recognizer r2 = new Recognizer("moveto 1 \n");
        Recognizer r3 = new Recognizer("move  \n");
        assertTrue( r.isBlock());
        assertTrue( r1.isMoveAction());
        assertFalse(r1.isMoveToAction());
        try {
            assertFalse(r2.isMoveToAction());
            fail();
        }
        catch (SyntaxException e) {
        }
        try {
            assertFalse(r3.isMoveAction());
            fail();
        }
        catch (SyntaxException e) {
        }
    }
   
    //<function call> ::= <NAME> <parameter list>
    @Test
    public void testIsFunctionCall() {
        Recognizer r = new Recognizer("foo(a,b,c)");
        assertTrue(r.isFunctionCall());
       
    }
    
    @Test
    //"define" <NAME> [ "using" <variable> { "," <variable> }  ] <block>
    //<block> ::= "{" <eol> { <command> }  "}" <eol>
    //<command> ::= <action>
    //		     | <statement>
    //  	     <statement> ::= <assignment statement>
    		//    | <loop statement>
			//    | <exit if statement>
			//    | <switch statement>
			//    | <return statement>
			//    | <do statement>
			//    | <color statement>
    public void testIsFunctionDefinition() {
        Recognizer r = new Recognizer("define foo using a,b,c { \n } \n ");
        assertTrue(r.isFunctionDefinition());
    }
    
    @Test
    public void testIsEol() {
        Recognizer r = new Recognizer("\n");
        assertTrue(r.isEol());
    }
    
    @Test
    public void testNextTokenMatchesType() {
        Recognizer r = new Recognizer("++abc");
        assertTrue(r.nextTokenMatches(Token.Type.SYMBOL));
        assertFalse(r.nextTokenMatches(Token.Type.NAME));
        assertTrue(r.nextTokenMatches(Token.Type.SYMBOL));
        assertTrue(r.nextTokenMatches(Token.Type.NAME));
    }

    @Test
    public void testNextTokenMatchesTypeString() {
        Recognizer r = new Recognizer("+abc+");
        assertTrue(r.nextTokenMatches(Token.Type.SYMBOL, "+"));
        assertTrue(r.nextTokenMatches(Token.Type.NAME, "abc"));
        assertFalse(r.nextTokenMatches(Token.Type.SYMBOL, "*"));
        assertTrue(r.nextTokenMatches(Token.Type.SYMBOL, "+"));
    }

    @Test
    public void testNextToken() {
        // NAME, KEYWORD, NUMBER, SYMBOL, EOL, EOF };
        Recognizer r = new Recognizer("abc move 25 *\n");
        assertEquals(new Token(Token.Type.NAME, "abc"), r.nextToken());
        assertEquals(new Token(Token.Type.KEYWORD, "move"), r.nextToken());
        assertEquals(new Token(Token.Type.NUMBER, "25.0"), r.nextToken());
        assertEquals(new Token(Token.Type.SYMBOL, "*"), r.nextToken());
        assertEquals(new Token(Token.Type.EOL, "\n"), r.nextToken());
        assertEquals(new Token(Token.Type.EOF, "EOF"), r.nextToken());
        
        r = new Recognizer("foo.bar 123.456");
        assertEquals(new Token(Token.Type.NAME, "foo"), r.nextToken());
        assertEquals(new Token(Token.Type.SYMBOL, "."), r.nextToken());
        assertEquals(new Token(Token.Type.NAME, "bar"), r.nextToken());
        assertEquals(new Token(Token.Type.NUMBER, "123.456"), r.nextToken());
    }

    @Test
    public void testPushBack() {
        Recognizer r = new Recognizer("abc 25");
        assertEquals(new Token(Token.Type.NAME, "abc"), r.nextToken());
        r.pushBack();
        assertEquals(new Token(Token.Type.NAME, "abc"), r.nextToken());
        assertEquals(new Token(Token.Type.NUMBER, "25.0"), r.nextToken());
    }
    
//  ----- "Helper" methods

    /**
     * This method is given a String containing some or all of the
     * tokens that should yet be returned by the Tokenizer, and tests
     * whether the Tokenizer in fact has those Tokens. To succeed,
     * everything in the given String must still be in the Tokenizer,
     * but there may be additional (untested) Tokens to be returned.
     * This method is primarily to test whether rejected Tokens are
     * pushed back appropriately.
     * 
     * @param recognizer The Recognizer whose Tokenizer is to be tested.
     * @param expectedTokens The Tokens we expect to get from the Tokenizer.
     */
    private void followedBy(Recognizer recognizer, String expectedTokens) {
        int expectedType;
        int actualType;
        StreamTokenizer actual = recognizer.tokenizer;

        Reader reader = new StringReader(expectedTokens);
        StreamTokenizer expected = new StreamTokenizer(reader);
        expected.ordinaryChar('-');
        expected.ordinaryChar('/');

        try {
            while (true) {
                expectedType = expected.nextToken();
                if (expectedType == StreamTokenizer.TT_EOF) break;
                actualType = actual.nextToken();
                assertEquals(expectedType, actualType);
                if (actualType == StreamTokenizer.TT_WORD) {
                    assertEquals(expected.sval, actual.sval);
                }
                else if (actualType == StreamTokenizer.TT_NUMBER) {
                    assertEquals(expected.nval, actual.nval, 0.001);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
