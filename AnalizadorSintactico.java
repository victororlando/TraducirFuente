
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AnalizadorSintactico {

    AnalizadorLexico.Token token;
    List<AnalizadorLexico.Token> tokensList = new ArrayList<>();
    int cursor = 0;
    String[] tokensValidos;
    List<String> tokensPermitidos = new ArrayList<String>();
    boolean errorEncontrado;
    private int cantidadTab;
    FileWriter fichero = null;
    PrintWriter pw = null;

    public void analizarTraducirFuente() throws IOException {
        Path currentRelativePath = Paths.get("");
        String path = currentRelativePath.toAbsolutePath().toString();
        String output = path + "/output.txt";
        fichero = new FileWriter(output);
        pw = new PrintWriter(fichero);

        AnalizadorLexico al = new AnalizadorLexico();
        tokensList = al.analizarFuente();
        //al.analizarFuente();
        getToken();
        json(new String[]{"EOF"});

        if (!errorEncontrado) {
            System.out.println("El código fuente es sintacticamente correcto");
        }
        try {
            if (null != fichero) {
                fichero.close();
            }
        } catch (Exception e2) {
            e2.printStackTrace();
        }
				System.out.println("Archivo output.txt generado");
        System.exit(0);

    }

    public void check_input(String[] firsts, String[] follows) {
        if (!existeEnArray(token.getComponenteLexico(), firsts)) {
            tokensValidos = firsts;
            for (int i = 0; i < firsts.length; i++) {
                if (!tokensPermitidos.contains(i)) {
                    tokensPermitidos.add(firsts[i]);
                }
            }
            error();
            scanto(union(firsts, follows));
        }
    }

    public boolean existeEnArray(String token, String[] array) {
        for (int i = 0; i < array.length; i++) {
            if (token == componenteLexico(array[i])) {
                return true;
            }
        }
        return false;
    }

    public void error() {
        System.out.println("Se produjo un error al leer el token: " + token + ", Se esperaba uno de los sgtes: ( " + tokensEsperados() + ")");
        errorEncontrado = true;
    }

    public void scanto(String[] array) {
        while (!existeEnArray(token.getComponenteLexico(), array)) {
            getToken();
        }
    }

    public AnalizadorLexico.Token getToken() {
        if (cursor < tokensList.size()) {
            token = tokensList.get(cursor++);
//            System.out.println(token);
        }
        return token;
    }

    public void match(String t) {
        if (token.getComponenteLexico() == null ? componenteLexico(t) == null : token.getComponenteLexico().equals(componenteLexico(t))) {
            getToken();
        } else {
            error();
        }
    }

    public String[] union(String[] first, String[] follow) {

        String[] unionArray = new String[first.length + follow.length + 1];

        for (int i = 0; i < first.length; i++) {
            unionArray[i] = first[i];
        }

        int pos = first.length;
        for (int i = 0; i < follow.length; i++) {
            unionArray[pos] = follow[i];
            pos++;
        }
        unionArray[pos] = "EOF";
        return unionArray;
    }

    public void json(String[] synchset) {
        check_input(new String[]{"{", "["}, synchset);
        if (!existeEnArray(token.getComponenteLexico(), synchset)) {
            element(new String[]{"EOF", ",", "]", "}"});
            match("EOF");
            check_input(synchset, new String[]{"{", "["});
        }
    }

    public void element(String[] synchset) {
        check_input(new String[]{"{", "["}, synchset);
        if (!existeEnArray(token.getComponenteLexico(), synchset)) {
            switch (token.getComponenteLexico()) {
                case "L_LLAVE":
                    objeto(new String[]{"EOF", ",", "]", "}"});
                    break;
                case "L_CORCHETE":
                    array(new String[]{"EOF", ",", "]", "}"});
                    break;
                default:
                    error();
            }
            check_input(synchset, new String[]{"{", "["});
        }
    }

    public void objeto(String[] synchset) {
        check_input(new String[]{"{"}, synchset);
        if (!existeEnArray(token.getComponenteLexico(), synchset)) {
            String tabString = "\t";
            for (int i = 0; i < cantidadTab; i++) {
                tabString = tabString + "\t";
            }
            switch (token.getComponenteLexico()) {
                case "L_LLAVE":
                    match("{");
                    System.out.println("\n" + tabString + "<item>");
                    pw.println("\n" + tabString + "<item>");
                    cantidadTab++;
                    attribute_list(new String[]{"}"});
                    System.out.println(tabString + "</item>");
                    pw.println(tabString + "</item>");
                    match("}");
                    break;
                case "R_LLAVE":
                    System.out.println(tabString + "<item>");
                    pw.println(tabString + "<item>");
                    match("}");
                    break;
                default:
                    error();
            }
            check_input(synchset, new String[]{"{"});
        }
    }

    public void array(String[] synchset) {
        check_input(new String[]{"["}, synchset);
        if (!existeEnArray(token.getComponenteLexico(), synchset)) {
            switch (token.getComponenteLexico()) {
                case "L_CORCHETE":
                    match("[");
                    element_list(new String[]{"]"});
                    match("]");
                    break;
                case "R_CORCHETE":
                    match("]");
                    break;
                default:
                    error();
            }
            check_input(synchset, new String[]{"["});
        }
    }

    public void element_list(String[] synchset) {
        check_input(new String[]{"{", "["}, synchset);
        if (!existeEnArray(token.getComponenteLexico(), synchset)) {
            element(new String[]{"EOF", ",", "]", "}"});
            while (token.getComponenteLexico() == null ? componenteLexico(",") == null : token.getComponenteLexico().equals(componenteLexico(","))) {
                match(",");
                element(new String[]{"EOF", ",", "]", "}"});
            }
            check_input(synchset, new String[]{"{", "["});
        }
    }

    public void attribute_list(String[] synchset) {
        check_input(new String[]{"String"}, synchset);
        if (!existeEnArray(token.getComponenteLexico(), synchset)) {
            attibute(new String[]{",", "}"});
            while (token.getComponenteLexico() == null ? componenteLexico(",") == null : token.getComponenteLexico().equals(componenteLexico(","))) {
                match(",");
                attibute(new String[]{",", "}"});
            }
            cantidadTab = cantidadTab - 1;
            check_input(synchset, new String[]{"String"});
        }
    }

    public void attibute(String[] synchset) {
        check_input(new String[]{"String"}, synchset);
        if (!existeEnArray(token.getComponenteLexico(), synchset)) {
            String lex = token.getLexema();
            String tabString = "\t";
            for (int i = 0; i < cantidadTab; i++) {
                tabString = tabString + "\t";
            }
            System.out.print(tabString + "<" + lex + ">");
            pw.print(tabString + "<" + lex + ">");
            att_name(new String[]{":"});
            match(":");
            System.out.print(token.getLexema());
            pw.print(token.getLexema());

            att_valor(new String[]{",", "}"});

            if (token.getComponenteLexico().equals("L_CORCHETE") || token.getComponenteLexico().equals("R_LLAVE")) {
                System.out.println(tabString + "</" + lex + ">");
                pw.print(tabString + "</" + lex + ">");
                pw.print("\n");
            } else {
                System.out.println("</" + lex + ">");
                pw.print("</" + lex + ">");
                pw.print("\n");

            }
            check_input(synchset, new String[]{"String"});
        }
    }

    public void att_name(String[] synchset) {
        check_input(new String[]{"String"}, synchset);
        if (!existeEnArray(token.getComponenteLexico(), synchset)) {
            match("String");
            check_input(synchset, new String[]{"String"});
        }
    }

    public void att_valor(String[] synchset) {
        check_input(new String[]{"{", "[", "String", "Num", "true", "false", "null"}, synchset);
        if (!existeEnArray(token.getComponenteLexico(), synchset)) {
            switch (token.getComponenteLexico()) {
                case "L_CORCHETE":
                    element(new String[]{"EOF", ",", "]", "}"});
                    break;
                case "L_LLAVE":
                    element(new String[]{"EOF", ",", "]", "}"});
                    break;
                case "LITERAL_CADENA":
                    match("String");
                    break;
                case "LITERAL_NUM":
                    match("Num");
                    break;
                case "PR_TRUE":
                    match("true");
                    break;
                case "PR_FALSE":
                    match("false");
                    break;
                case "PR_NULL":
                    match("null");
                    break;
                default:
                    error();
            }
            check_input(synchset, new String[]{"[", "{", "String", "Num", "true", "false", "null"});
        }
    }

    public String componenteLexico(String s) {
        switch (s) {
            case "{":
                return "L_LLAVE";
            case "}":
                return "R_LLAVE";
            case "[":
                return "L_CORCHETE";
            case "]":
                return "R_CORCHETE";
            case ",":
                return "COMA";
            case ":":
                return "DOS_PUNTOS";
            case "Num":
                return "LITERAL_NUM";
            case "true":
                return "PR_TRUE";
            case "false":
                return "PR_FALSE";
            case "null":
                return "PR_NULL";
            case "String":
                return "LITERAL_CADENA";
            case "EOF":
                return "EOF";
        }
        return null;
    }

    private String tokensEsperados() {
        String s = "";
        for (int i = 0; i < tokensValidos.length; i++) {
            s = s + "\"" + tokensValidos[i] + "\" ";
        }
        return s;
    }

}
