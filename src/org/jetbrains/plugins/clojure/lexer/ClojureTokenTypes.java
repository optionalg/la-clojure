package org.jetbrains.plugins.clojure.lexer;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.TokenType;
import org.jetbrains.plugins.clojure.parser.ClojureElementType;

/**
 * User: peter
 * Date: Nov 20, 2008
 * Time: 1:50:48 PM
 * Copyright 2007, 2008, 2009 Red Shark Technology
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public interface ClojureTokenTypes {

    // Special characters
    IElementType LEFT_PAREN = new ClojureElementType("(");
    IElementType RIGHT_PAREN = new ClojureElementType(")");

    IElementType LEFT_CURLY = new ClojureElementType("{");
    IElementType RIGHT_CURLY = new ClojureElementType("}");

    IElementType LEFT_SQUARE = new ClojureElementType("[");
    IElementType RIGHT_SQUARE = new ClojureElementType("]");

    IElementType POUND = new ClojureElementType("#");
    IElementType UP = new ClojureElementType("^");
    IElementType POUNDUP = new ClojureElementType("#^");
    IElementType TILDA = new ClojureElementType("~");
    IElementType AT = new ClojureElementType("@");
    IElementType TILDAAT = new ClojureElementType("~@");
    IElementType PERCENT = new ClojureElementType("%");
    IElementType QUOTE = new ClojureElementType("'");
    IElementType BACKQUOTE = new ClojureElementType("`");

    // Comments
    IElementType LINE_COMMENT = new ClojureElementType("line comment");
    TokenSet COMMENTS = TokenSet.create(LINE_COMMENT);

    // Literals
    IElementType STRING_LITERAL = new ClojureElementType("string literal");
    IElementType INTEGER_LITERAL = new ClojureElementType("numeric literal");
    IElementType FLOAT_LITERAL = new ClojureElementType("numeric literal");
    IElementType CHAR_LITERAL = new ClojureElementType("character literal");

    IElementType T = new ClojureElementType("t");
    IElementType NIL = new ClojureElementType("nil");
    TokenSet BOOLEAN_LITERAL = TokenSet.create(T, NIL);

    TokenSet LITERALS = TokenSet.create(STRING_LITERAL, INTEGER_LITERAL, FLOAT_LITERAL, CHAR_LITERAL, T, NIL);

    TokenSet READABLE_TEXT = TokenSet.create(STRING_LITERAL, LINE_COMMENT);

    IElementType SYMBOL = new ClojureElementType("symbol"); // foo
    IElementType COLON_SYMBOL = new ClojureElementType("key");  // :foo

    // Control characters
    IElementType EOL = new ClojureElementType("end of line");
    IElementType EOF = new ClojureElementType("end of file");
    IElementType WHITESPACE = TokenType.WHITE_SPACE;
    IElementType BAD_CHARACTER = TokenType.BAD_CHARACTER;

    TokenSet WHITESPACE_SET = TokenSet.create(EOL, EOF, WHITESPACE);
    TokenSet STRINGS = TokenSet.create(STRING_LITERAL);
}