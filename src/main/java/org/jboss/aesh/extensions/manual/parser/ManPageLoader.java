/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.extensions.manual.parser;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.extensions.manual.ManPage;
import org.jboss.aesh.extensions.page.PageLoader;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Read a asciidoc file and parse it to something that can be
 * displayed nicely in a terminal.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ManPageLoader implements PageLoader {

    private List<ManSection> sections;
    private String name;
    private String fileName;
    private String headerText;
    private InputStreamReader reader;

    public ManPageLoader() {
        sections = new ArrayList<ManSection>();
    }

    /**
     * Read from a specified filename. Also supports gzipped files.
     *
     * @param filename File
     * @throws IOException
     */
    public void setFile(String filename) throws IOException {
        setFile(new File(filename));
    }

    /**
     * Read from a specified file. Also supports gzipped files.
     *
     * @param file File
     * @throws IOException
     */
    public void setFile(File file) throws IOException {
        if(!file.isFile())
            throw new IllegalArgumentException(file+" must be a file.");
        else {
            if(file.getName().endsWith("gz"))
                initGzReader(file);
            else
                initReader(file);
            sections.clear();
        }
    }

    /**
     * Read a file resouce located in a jar
     *
     * @param fileName name
     */
    public void setFileFromAJar(String fileName) {
        InputStream is = this.getClass().getResourceAsStream(fileName);
        if(is != null) {
            this.fileName = fileName;
            reader = new InputStreamReader(is);
        }
    }

    private void initReader(File file) throws FileNotFoundException {
        fileName = file.getAbsolutePath();
        reader = new FileReader(file);
    }

    private void initGzReader(File file) throws IOException {
        fileName = file.getAbsolutePath();
        GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(file));
        reader = new InputStreamReader(gzip);
    }

    public String getResourceName() {
        return fileName;
    }

    public String getName() {
        return name;
    }

    @Override
    public List<String> loadPage(int columns) throws IOException {
        //we already have the file loaded
        if(!sections.isEmpty())
            return getAsList();
        if(reader == null)
            throw new IOException("InputStreamReader is null, cannot read file.");
        //parse the file
        BufferedReader br = new BufferedReader(reader);
        try {
            String line = br.readLine();
            boolean foundEmptyLine = true;
            List<String> section = new ArrayList<String>();
            while (line != null) {
                if(line.trim().isEmpty() && !foundEmptyLine) {
                    foundEmptyLine = true;
                    section.add(line);
                }
                else if(line.isEmpty() && foundEmptyLine) {
                    ManSection manSection = new ManSection().parseSection(section, columns);
                    sections.add(manSection);
                    foundEmptyLine = false;
                    section.clear();
                }
                else {
                    if(foundEmptyLine)
                        foundEmptyLine = false;
                    section.add(line);
                }

                line = br.readLine();
            }
            if(!section.isEmpty()) {
                ManSection manSection = new ManSection().parseSection(section, columns);
                sections.add(manSection);
            }

            processHeader(columns);

            return getAsList();
        }
        finally {
            br.close();
        }
    }

    //TODO: create a better header
    private void processHeader(int columns) {
        name = sections.get(0).getName();
        sections.remove(0);
    }

    public List<ManSection> getSections() {
        return sections;
    }

    public List<String> getAsList() {
        List<String> out = new ArrayList<String>();
        for(ManSection section : sections)
            out.addAll(section.getAsList());

        return out;
    }

    public String print() {
        StringBuilder builder = new StringBuilder();
        for(ManSection section : sections) {
            builder.append(section.printToTerminal()).append(Config.getLineSeparator());
        }

        return builder.toString();
    }
}