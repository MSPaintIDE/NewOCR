package com.uddernetworks.newocr.recognition.similarity;

import com.uddernetworks.newocr.character.ImageLetter;

import java.util.Arrays;

public enum Letter {
    EXCLAMATION('!', 0, "|"),
    EXCLAMATION_DOT('!', 1, "."),
    QUOTE_LEFT('\"', 0),
    QUOTE_RIGHT('\"', 1),
    HASHTAG('#'),
    DOLLAR('$'),
    PERCENT_BASE('%', 1),
    PERCENT_LDOT('%', 0),
    PERCENT_RDOT('%', 2),
    AMPERSAND('&'),
    APOSTROPHE('\''),
    LEFT_PARENTHESE('('),
    RIGHT_PARENTHESE(')'),
    ASTERISK('*'),
    PLUS('+'),
    COMMA(','),
    MINUS('-'),
    PERIOD('.'),
    FORWARD_SLASH('/'),
    ZERO('0'),
    ONE('1'),
    TWO('2'),
    THREE('3'),
    FOUR('4'),
    FIVE('5'),
    SIX('6'),
    SEVEN('7'),
    EIGHT('8'),
    NINE('9'),
    COLON_TOP(':', 0),
    COLON_BOTTOM(':', 1),
    SEMICOLON_TOP(';', 1),
    SEMICOLON_BOTTOM(';', 0),
    LESS_THAN('<'),
    EQUALS_TOP('=', 0),
    EQUALS_BOTTOM('=', 1),
    GREATER_THAN('>'),
    QUESTION_MARK_TOP('?', 0),
    QUESTION_MARK_BOTTOM('?', 1),
    AT('@'),
    A('A'),
    B('B'),
    C('C'),
    D('D'),
    E('E'),
    F('F'),
    G('G'),
    H('H'),
    I('I'),
    J('J'),
    K('K'),
    L('L'),
    M('M'),
    N('N'),
    O('O'),
    P('P'),
    Q('Q'),
    R('R'),
    S('S'),
    T('T'),
    U('U'),
    V('V'),
    W('W'),
    X('X'),
    Y('Y'),
    Z('Z'),
    LEFT_SQUARE_BRACKET('['),
    BACKSLASH('\\'),
    RIGHT_SQUARE_BRACKET(']'),
    CARROT('^'),
    UNDERSCORE('_'),
    GRAVE('`'),
    a('a'),
    b('b'),
    c('c'),
    d('d'),
    e('e'),
    f('f'),
    g('g'),
    h('h'),
    i_DOT('i', 0),
    i('i', 1),
    j_DOT('j', 0),
    j('j', 1),
    k('k'),
    l('l'),
    m('m'),
    n('n'),
    o('o'),
    p('p'),
    q('q'),
    r('r'),
    s('s'),
    t('t'),
    u('u'),
    v('v'),
    w('w'),
    x('x'),
    y('y'),
    z('z'),
    LEFT_CURLY_BRACKET('{'),
    PIPE('|'),
    RIGHT_CURLY_BRACKET('}'),
    TILDE('~'),
    SPACE(' ');

    private final char letter;
    private final int mod;
    private final String print;

    Letter(char letter) {
        this(letter, 0);
    }

    Letter(char letter, int mod) {
        this(letter, mod, String.valueOf(letter));
    }

    Letter(char letter, int mod, String print) {
        this.letter = letter;
        this.mod = mod;
        this.print = print;
    }

    public char getLetter() {
        return letter;
    }

    public int getMod() {
        return mod;
    }

    public boolean matches(ImageLetter imageLetter) {
        return matches(imageLetter.getLetter(), imageLetter.getModifier());
    }

    public boolean matches(char letter, int mod) {
        return letter == this.letter && mod == this.mod;
    }

    public static Letter getLetter(ImageLetter imageLetter) {
        return getLetter(imageLetter.getLetter(), imageLetter.getModifier());
    }

    public static Letter getLetter(char character, int mod) {
        var optional = Arrays.stream(values())
                .filter(letter -> letter.letter == character && letter.mod == mod)
                .findFirst();
        if (optional.isEmpty()) {
            System.err.println("Couldn't find shit with " + character + " and " + mod);
            return SPACE;
        }

        return optional.get();
    }

    @Override
    public String toString() {
        return this.print;
    }
}
