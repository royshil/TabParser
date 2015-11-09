package com.royshilkrot.tabgenerator;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.DefaultConfiguration;

/**
 *    Textual guitar tab parser
 */
public class TabParser {
    private Logger log = LogManager.getLogger(TabParser.class);

    private ArrayList<TabLine> lines;

	public static class TabNote {
        public enum Type {
            NOTE,
            BAR
            //TODO: add more types of marking in a tab
            //https://en.wikipedia.org/wiki/Tablature#Guitar_tablature
        }
        
        public String note;
        public int octave;
        public Type type;
    }
    
    public class TabLine {        
        public TabLine() {
        }
        public TabLine(int start, int end) {
            this.start = start;
            this.end = end;
        }
        public int start;
        public int end;
        
        public ArrayList<String>                     lines = new ArrayList<String>();
        public ArrayList<HashMap<Integer,TabNote>> columns = new ArrayList<HashMap<Integer,TabNote>>();
        
        private final String[] notescale = new String[] {
            "E", "F", "F#", "G", "G#", "A", "B", "C", "C#", "D", "D#"
        };
        
        /**
         * Process the consumed lines. Parse each line into the internal representation.
         */
        public void processLines() {
            log.debug("process line");
            if (lines.size() != 6) {
                log.error("TabLine assumes 6 staff lines");
                return;
            }
            int lineLength = lines.get(0).length();
            char[] lineE = lines.get(5).toCharArray();
            char[] lineA = lines.get(4).toCharArray();
            char[] lineD = lines.get(3).toCharArray();
            char[] lineG = lines.get(2).toCharArray();
            char[] lineB = lines.get(1).toCharArray();
            char[] linee = lines.get(0).toCharArray();
            for (int i = 0; i < lineLength; i++) {
                if (lineE[i] == '|') {
                    log.debug("skipping column " + i);
                }
                processColumn(lineE[i], lineA[i], lineD[i], lineG[i], lineB[i], linee[i]);
            }
        }
        
        /**
         * find the note for a given fret on a string 
         * @param c				The fret ('0' -> ~'12')
         * @param octaveCutoff	Where on the string (what fret) the octave changes from low to high
         * @param highOctave	What is the high octave  on this string
         * @param lowOctave		What is the low octave on this string
         * @param scaleOffset	Where in the notes array (CDEFGAB) does this string start
         * @return				The note and octave
         */
        TabNote processChar(char c, int octaveCutoff, int highOctave, int lowOctave, int scaleOffset) {
            TabNote note = new TabNote();
            int cn = c - '0';
            note.octave = (cn <= octaveCutoff) ? lowOctave : highOctave;
            note.note = notescale[(scaleOffset + cn) % 11];
            note.type = TabNote.Type.NOTE;
            return note;
        }
        
        /**
         * Assuming a standard tuning: E2 A2 D3 G3 B3 E4
         * https://en.wikipedia.org/wiki/Standard_tuning
         * 
         * Process a column of the tab line.
         */
        private void processColumn(char E, char A, char D, char G, char B, char e) {
            log.debug("process column: " + E +" " + A + " " + D + " " + G + " " + B + " " + e);
            HashMap<Integer,TabNote> notesInColumn = new HashMap<Integer, TabParser.TabNote>();
            if (E == '|') {
                //TODO assuming all column will be '|', but needs checking
                TabNote note = new TabNote();
                note.type = TabNote.Type.BAR;
                notesInColumn.put(0, note);
                notesInColumn.put(1, note);
                notesInColumn.put(2, note);
                notesInColumn.put(3, note);
                notesInColumn.put(4, note);
                notesInColumn.put(5, note);
            } else {
	            if (Character.isDigit(E)) {
	                notesInColumn.put(0, processChar(E, 7, 3, 2, 0));
	            }
	            if (Character.isDigit(A)) {
	                notesInColumn.put(1, processChar(A, 2, 3, 2, 5));
	            }
	            if (Character.isDigit(D)) {
	                notesInColumn.put(2, processChar(D, 9, 4, 3, 9));
	            }
	            if (Character.isDigit(G)) {
	                notesInColumn.put(3, processChar(G, 4, 4, 3, 3));
	            }
	            if (Character.isDigit(B)) {
	                notesInColumn.put(4, processChar(B, 0, 4, 3, 6));
	            }
	            if (Character.isDigit(e)) {
	                notesInColumn.put(5, processChar(e, 7, 5, 4, 0));
	            }
            }
            if (!notesInColumn.isEmpty()) {
                columns.add(notesInColumn);
            }
        }
        
        /**
         * Render the internal representation to a tab-like format string.
         * Each string will be printed, but instead of finger-placing it will say what note it is
         * @return The note-tab-formatted tab line
         */
        public String printTabLine() {
            String out = "";
            for (int i = 5; i >= 0; i--) {
//                out += "|-";
                for (HashMap<Integer,TabNote> col : columns) {
                    TabNote tabNote = col.get(i);
                    if (tabNote == null) {
                    	out += "---";
                    } else if (tabNote.type == TabNote.Type.NOTE) { 
                        out += tabNote.note + tabNote.octave;
                        if (!tabNote.note.contains("#")) {
                        	out += "--";
                        }
                    } else if (tabNote.type == TabNote.Type.BAR) {
                        out += "|";
                    }
                }
                out += "\n";
            }
            return out;
        }
        
        @Override
        public String toString() {
            return "[TabLine: " + start + " -> " + end + "]";
        }
    }
    
    public static void main( String[] args ) throws IOException {
        System.setProperty("org.apache.logging.log4j.level", "INFO");
//        Configurator.initialize(new DefaultConfiguration());
        
        TabParser parser = new TabParser();
        String tab = "  |       |       |       |         |       |       |       |\n"
                +"|---------------------------------|---------------------------------|\n"
                +"|-----0---0---0-------0---0---0---|-----1-0-1---1-------1-0-1---1---|\n"
                +"|-------2---------------2---------|---------------------------------|\n"
                +"|---0-------0---0---0-------0---0-|---2-------2---2---2-------2---2-|\n"
                +"|---------------------------------|---------------------------------|\n"
                +"|-3---------------3---------------|-3---------------3---------------|\n"
                +"                                                                     \n"
                +"                                                                     \n"
                +"  |       |       |       |         |       |       |       |        \n"
                +"|---------------------------------|---------------------------------|\n"
                +"|-----1-0-1---1-------1-0-1---1---|---------0---0-----------0---0---|\n"
                +"|---------------------------------|---0-2-0---0---0---0-2-0---0---0-|\n"
                +"|---4-------4---4---4-------4---4-|---------------------------------|\n"
                +"|---------------------------------|---------------------------------|\n"
                +"|-3---------------3---------------|-3---------------3---------------|\n"
                +"                                                                     \n"
                +"                                                                     \n"
                +"  |       |       |       |         |       |       |       |        \n"
                +"|---------------------------------|---------------------------------|\n"
                +"|-----0---0-----------------------|---------------------------------|\n"
                +"|-------2---0---0---0---0---------|---0-2-0-2-0-2-0---0-2-0-2-0-2-0-|\n"
                +"|---2---------4---2---4-----0-----|---------------------------------|\n"
                +"|-------------------------2---4-2-|-4---------------4---------------|\n"
                +"|-3-------------------------------|---------------------------------|\n"
                +"                                                                     \n"
                +"                                                                     \n"
                +"  |       |       |       |         |       |       |       |        \n"
                +"|---------------------------------|---------------------------------|\n"
                +"|-----3-2-3-----------------------|---------------------------------|\n"
                +"|---2-------2-0-2---2-0-2---------|-----0---0---0-------0---0---0---|\n"
                +"|-4---------------4-------0-4-2-0-|-------4---------------4---------|\n"
                +"|---------------------------------|---2-------2---2---2-------2---2-|\n"
                +"|---------------------------------|-0---------------0---------------|\n";
        parser.processTab(tab);
        
//        parser.processTab(IOUtils.toString(new URL("http://tabs.ultimate-guitar.com/l/led_zeppelin/stairway_to_heaven_tab.htm").openStream()));
        
        parser.exportLilypond(new PrintWriter(System.out));
    }
    
    public void exportLilypond(PrintWriter out) throws FileNotFoundException {
    	out.println("symbols = { \n"
    			+ "\\time 4/4 \n");
    	for (TabLine tabLine : lines) {
    		for (HashMap<Integer,TabNote> col : tabLine.columns) {
            	int numNotes = 0;
            	String notes = "";
            	for (int i = 5; i >= 0; i--) {
	                TabNote tabNote = col.get(i);
	                if (tabNote == null) {
	                	;
	                } else if (tabNote.type == TabNote.Type.NOTE) { 
	                    String lilyNote = tabNote.note.toLowerCase();
	                    if (lilyNote.contains("#")) {
	                    	//FIXME somehow lilyNote.replace("#","is") doesn't work...
	                    	lilyNote = lilyNote.substring(0, lilyNote.indexOf('#')) + "is";
	                    }
						notes += lilyNote;
						if (tabNote.octave == 2) {
							notes += ",";
						}
						if (tabNote.octave == 4) {
							notes += "'";
						}
						notes += " ";
						numNotes++;
	                } else if (tabNote.type == TabNote.Type.BAR) {
	                	notes += " | ";
	                	break; // no need to scan other columns
	                }
	            }
	            //grouped notes (on the same column) are bracketed
	            if (numNotes > 1) {
	            	out.print(" <");
	            }
	            out.print(notes.trim());
	            if (numNotes > 1) {
	            	out.print(">");
	            }
	            if (!notes.contains("|")) {
	            	out.print("16 ");
	            }
            }
    		out.println();
    	}
    	out.println("}");
    	out.println("\\score { \n"
    			+ "<< \n"
    			+ "\\new Staff { \\clef \"G_8\" \\symbols } \n"
    			+ "\\new TabStaff { \\symbols } \n"
    			+ ">> \n"
    			+ "}");
    	out.flush();
    }
    
    public void processTab(String tab) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(tab));
        String line = "";
        int linenum = 0;
        lines = new ArrayList<TabParser.TabLine>();
        int consecutive = 0;
        TabLine tabLine = null;
        while((line = reader.readLine()) != null) {
            int countDashes = StringUtils.countMatches(line, "-");
            if (countDashes > (line.length() / 2)) {
                if (consecutive == 0) {
                    tabLine = new TabLine();
                    tabLine.start = linenum;
                }
                consecutive++;
                tabLine.lines.add(line);
            }
            if (consecutive == 6) {
                tabLine.end = linenum;
                lines.add(tabLine);
                consecutive = 0;
            }
            linenum++;
        }
        
        for (TabLine tabline : lines) {
            System.out.println("found tab line: " + tabline.toString());
            tabline.processLines();
        }
        for (TabLine tabline : lines) {
            System.out.println(tabline.printTabLine());
        }
    }
}
