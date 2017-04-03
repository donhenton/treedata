/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dhenton9000.treedata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TreeData {

    private static final Logger LOG = LoggerFactory.getLogger(TreeData.class);
    private int idCounter = 0;

    /**
     * convert a classpath reference to a file on the drive system
     *
     * @param path
     * @return
     * @throws java.io.FileNotFoundException if the file at the path does not
     * exist
     */
    private File convertClassPathToFileRef(String path)
            throws FileNotFoundException {

        if (this.getClass().getResource(path) != null) {
            return new File(FileUtils.toFile(getClass()
                    .getResource(path)).getAbsolutePath());
        } else {
            String info
                    = String.format("unable to find file at '%s'", path);
            throw new FileNotFoundException(info);
        }
    }

    private int getNewId() {
        idCounter++;
        return idCounter;
    }

    public void doWork() {
        try {
            //proper usage pattern: java <filename> <delimiter>
            String inputFile = "/taxonomy.csv";
            String delimiter = ",";

            File f = convertClassPathToFileRef(inputFile);

            BufferedReader br = new BufferedReader(new FileReader(f));
            String catName;

            //main data structure for storage
            Map<String, Object> mainMap = new LinkedHashMap<String, Object>();
            Stack<String> nameStack = new Stack<String>();
            int lineCount = 0;
            String[] curString;
            LinkedHashMap<String, Object> curMap, tempMap;

            while ((catName = br.readLine()) != null) {
                catName = catName.trim();
                //ignore header lines in file
                if (catName.startsWith("#")) {
                    continue;
                }

                curString = catName.split(delimiter);
                if (lineCount == 0) {
                    //start the ball rolling
                    lineCount++;
                    int id = getNewId();
                    curMap = new LinkedHashMap<String, Object>();
                    curMap.put("name", cleanText(curString[0]));
                    curMap.put("uuid", id);
                    curMap.put("children", new ArrayList<Object>());
                    mainMap.put("" + id, curMap);
                    nameStack.push("" + id);
                    continue;
                }

                boolean skipIt = true;
                boolean foundMatch = false;
                while (!foundMatch) {
                    LinkedHashMap<String, Object> topParent = null;
                    if (nameStack.size() > 0) {
                        topParent
                                = (LinkedHashMap<String, Object>) mainMap.get(nameStack.peek());
                    } else {
                        curMap = new LinkedHashMap<String, Object>();
                        int id = getNewId();
                        curMap.put("name", cleanText(curString[0]));
                        curMap.put("uuid", id);
                        curMap.put("children", new ArrayList<Object>());
                        mainMap.put("" + id, curMap);
                        nameStack.push("" + id);
                        topParent = curMap;
                    }

                    for (String currentElem : curString) {
                        //get to the place that has the item on the top of the stack
                        if (currentElem.equals((String) topParent.get("name")) && skipIt) {
                            skipIt = false;

                            continue;
                        }
                        if (!skipIt) {
                            // tell the loop that you found something
                            foundMatch = true;
                            int id = getNewId();
                            tempMap = new LinkedHashMap<String, Object>();
                            tempMap.put("name", cleanText(currentElem));
                            tempMap.put("uuid", id);
                            tempMap.put("children", new ArrayList<Object>());
                            mainMap.put("" + id, tempMap);
                            nameStack.push("" + id);
                            ((ArrayList<Object>) topParent.get("children")).add(tempMap);
                        }
                    } //end for walk of line elements
                    if (skipIt == true) {
                        //nothing matched so pop the stack in hopes of finding a match
                        nameStack.pop();
                    }
                } // end while
                LOG.debug("finished Line " + lineCount);
                lineCount++;
                //if (lineCount > 100) {
               //     break;
               // }
            }//end loop for lines

            br.close();

            for (String t : mainMap.keySet()) {
                Map item = ((Map) mainMap.get(t));
                ArrayList<Object> children
                        = (ArrayList<Object>) item.get("children");
                boolean has_children = false;
                if (children != null && children.size() > 0) {
                    has_children = true;
                }
                item.put("has_children", has_children);
            }

            String jsonString = JSONValue.toJSONString(mainMap);

//            //default output filename: flare.json -- modify if necessary
            FileWriter writer = new FileWriter("flare.json");
            writer.write(jsonString);
            writer.close();

            // LOG.info(jsonString);
        } catch (Exception e) {
            LOG.error("Main Error " + e.getMessage(), e);

        }
    }

    private String cleanText(String t) {
        String res = t.replaceAll("\"", "");
        res = res.trim();
        return res;
    }

    public static void main(String[] args) {
        (new TreeData()).doWork();
    }

}
