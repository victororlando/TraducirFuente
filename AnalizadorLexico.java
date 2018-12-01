
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnalizadorLexico {

    static List<Character> charList = new ArrayList<>();
    static String numeros = "0,1,2,3,4,5,6,7,8,9";

    public List<Token> analizarFuente() throws FileNotFoundException, IOException {
        Path currentRelativePath = Paths.get("");
        String path = currentRelativePath.toAbsolutePath().toString();
        //Leemos archivo fuente
        String archivo = path + "/fuente.txt";
        FileReader f = new FileReader(archivo);
        BufferedReader fuente = new BufferedReader(f);
        int c;

        //guardamos en buffer una secuencia de simbolos separados por espacios 
        //a ser analizado posteriormente pasa saber a que componente lexico pertenece
        String buffer = "";
        while ((c = fuente.read()) != -1) {
            char ch = (char) c;
            charList.add(ch);
        }
        Token t = null;
        List<Token> tokensList = new ArrayList<>();
        for (int i = 0; i < charList.size(); i++) {
            char simbolo = leerSimbolo(i);
            String simboloAnalizado = analizarToken(simbolo);
            if (simboloAnalizado != null && buffer.isEmpty()) {
                t = new Token(simboloAnalizado, "");
                tokensList.add(t);
            } else if (simbolo != '"' && !esEspacioVacioTabEspacioSalto(simbolo) && simboloAnalizado == null) {
                buffer = buffer + simbolo;
                if (!buffer.isEmpty()) {
                    int sigIndex = i + 1;
                    if (sigIndex < charList.size()) {
                        String sigToken = analizarToken(leerSimbolo(sigIndex));
                        if (sigToken != null || bufferCompleto(leerSimbolo(sigIndex))) {
                            simboloAnalizado = analizarToken(buffer);
                            if (simboloAnalizado != null || simbolo == '"') {
                                t = new Token(simboloAnalizado, buffer);
                                tokensList.add(t);
                                buffer = "";
                            }
                        }
                    }
                }
            } else {
            }
        }
        fuente.close();
        t = new Token("EOF", "EOF");
        tokensList.add(t);
        return tokensList;
    }

    //Este metodo retorna el componente lexico del lexema
    public String analizarToken(Object lex) {
        if (lex instanceof Character) {
            switch ((Character) lex) {
                case '{':
                    return "L_LLAVE";
                case '}':
                    return "R_LLAVE";
                case '[':
                    return "L_CORCHETE";
                case ']':
                    return "R_CORCHETE";
                case ',':
                    return "COMA";
                case ':':
                    return "DOS_PUNTOS";

            }
        } else if (lex instanceof String) {
            String s = (String) lex;
            Pattern pat = null;
            Matcher mat = null;

            //Patron para las palabras true y TRUE
            pat = Pattern.compile("true|TRUE");
            mat = pat.matcher((String) lex);
            if (mat.matches()) {
                return "PR_TRUE";
            }

            //Patron para las palabras false y FALSE
            pat = Pattern.compile("false|FALSE");
            mat = pat.matcher((String) lex);
            if (mat.matches()) {
                return "PR_FALSE";
            }

            //Patron para las palabras null y NULL
            pat = Pattern.compile("null|NULL");
            mat = pat.matcher((String) lex);
            if (mat.matches()) {
                return "PR_NULL";
            }

            //Numeros
            TipoDigito tipoDigito = new TipoDigito(s);

            if (esDigito(tipoDigito.getValor())) {
                int i = 0;
                int estado = 0;
                boolean acepto = false;

                while (!acepto) {
                    switch (estado) {
                        case 0: //una secuencia netamente de digitos, puede ocurrir . o e
                            String c = tipoDigito.getValor();
                            if (esDigito(c)) {
                                estado = 0;
                            } else if (c.equals(".")) {
                                estado = 1;
                            } else if ("E".equals(c.toUpperCase())) {
                                estado = 3;
                            } else if (tipoDigito.getIndex() == tipoDigito.getSize()) {
                                estado = 6;
                            }
                            break;

                        case 1://un punto, debe seguir un digito
                            c = tipoDigito.getValor();
                            if (esDigito(c)) {
                                estado = 2;
                            } else {
                                System.out.println("No se esperaba: " + c + " " + lex);
                                estado = -1;
                            }
                            break;
                        case 2://la fraccion decimal, pueden seguir los digitos o e
                            c = tipoDigito.getValor();
                            if (esDigito(c)) {
                                estado = 2;
                            } else if ("E".equals(c.toUpperCase())) {
                                estado = 3;
                            } else {
                                estado = 6;
                            }
                            break;
                        case 3://una e, puede seguir +, - o una secuencia de digitos
                            c = tipoDigito.getValor();
                            if ("+".equals(c) || "-".equals(c)) {
                                estado = 4;
                            } else if (esDigito(c)) {
                                estado = 5;
                            } else {
                                System.out.println("No se esperaba: " + c + " " + lex);
                                estado = -1;
                            }
                            break;
                        case 4://necesariamente debe venir por lo menos un digito
                            c = tipoDigito.getValor();
                            if (esDigito(c)) {
                                estado = 5;
                            } else {
                                System.out.println("No se esperaba: " + c + " " + lex);
                                estado = -1;
                            }
                            break;
                        case 5://una secuencia de digitos correspondiente al exponente
                            c = tipoDigito.getValor();
                            if (esDigito(c)) {
                                estado = 5;
                            } else {
                                estado = 6;
                            }
                            break;
                        case 6:
                            acepto = true;
                            break;
                        case -1:
                            System.out.println("-1");
                            return "ERROR";
                    }
                }

                if (acepto) {
                    return "LITERAL_NUM";
                } else {

                }
            }

            //Patron para cualquier caracter
            pat = Pattern.compile(".*");
            mat = pat.matcher((String) lex);
            if (mat.matches()) {
                return "LITERAL_CADENA";
            }

            System.out.println("El valor: " + lex + " no concuerda con ningun patron permitido");

        }
        return null;
    }

    private boolean esDigito(String d) {
        return numeros.contains(d);
    }

    public char leerSimbolo(int posicion) {
        return charList.get(posicion);
    }

    public boolean esEspacioVacioTabEspacioSalto(Character c) {
        if (c == ' ' || c == '"' || c == '\t' || c == '\r' || c == '\n') {
            return true;
        }
        return false;
    }

    public boolean bufferCompleto(Character c) {
        if (c == '"' || c == '\t' || c == '\r' || c == '\n') {
            return true;
        }
        return false;
    }

    //Clase que maneja un digito como un string
    private class TipoDigito {

        String valor;
        int index;
        int size = 0;

        public TipoDigito(String valor) {
            this.valor = valor;
            this.index = 0;
            this.size = valor.length();
        }

        public String getValor() {
            String retorno = null;
            if (index < size) {
                retorno = valor.charAt(index) + "";
                index++;
            } else {
                return "Finalizacion Numero";
            }
            return retorno;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

    }

    class Token {

        String componenteLexico;
        String lexema;

        public Token(String componenteLexico, String lexma) {
            this.componenteLexico = componenteLexico;
            this.lexema = lexma;
        }

        public String getComponenteLexico() {
            return componenteLexico;
        }

        public void setComponenteLexico(String componenteLexico) {
            this.componenteLexico = componenteLexico;
        }

        public String getLexema() {
            return lexema;
        }

        public void setLexema(String lexema) {
            this.lexema = lexema;
        }

        @Override
        public String toString() {
            return "Token=>" + "componenteLexico=" + componenteLexico + ", lexema=" + lexema;
        }

    }

}
