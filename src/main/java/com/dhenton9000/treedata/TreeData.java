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
import java.util.List;
import java.util.Map;
import org.apache.commons.io.FileUtils;

import org.json.simple.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TreeData {

    private static final Logger LOG = LoggerFactory.getLogger(TreeData.class);

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

    public void doWork() {
        try {
            //proper usage pattern: java <filename> <delimiter>
            String inputFile = "/taxonomy.txt";
            String delimiter = ">";

            File f = convertClassPathToFileRef(inputFile);

            BufferedReader br = new BufferedReader(new FileReader(f));
            String catName;

            //main data structure for storage
            Map<String, Object> flareMap = new LinkedHashMap<String, Object>();
            flareMap.put("name", "flare");
            flareMap.put("children", new ArrayList<Object>());

            String[] curString;
            Map<String, Object> curMap, tempMap;

            while ((catName = br.readLine()) != null) {
                catName = catName.trim();
                //ignore header lines in file
                if (catName.startsWith("#")) {
                    continue;
                }

                curString = catName.split(delimiter);

                //if the category is not found in map, create a new path
                if ((tempMap = findName(flareMap, curString[0])) == null) {
                    int i = 0;
                    tempMap = flareMap;
                    //create nodes for each subcategory in string
                    while (i < curString.length) {
                        tempMap = createNode(tempMap, curString[i++]);
                    }
                } else {
                    //otherwise, it will be an existing category name in path,
                    //iterate until last location and create nodes for subcategories
                    int i = 0;
                    tempMap = flareMap;
                    while (i < curString.length
                            && (curMap = findName(tempMap, curString[i])) != null) {
                        i++;
                        tempMap = curMap;
                    }
                    while (i < curString.length) {
                        tempMap = createNode(tempMap, curString[i++]);
                    }
                }
            }

            br.close();
            String jsonString = JSONValue.toJSONString(flareMap);

            //default output filename: flare.json -- modify if necessary
            FileWriter writer = new FileWriter("flare.json");
            writer.write(jsonString);
            writer.close();

            LOG.info(jsonString);
        } catch (Exception e) {
            LOG.error("Error! Correct usage: <filename> <delimiter>",e);
             
        }
    }

    public static void main(String[] args) {
        (new TreeData()).doWork();
    }

    /**
     * Creates a new node for a category not already in the map.
     *
     * @param current the current map to add to
     * @param in the new category name to be added
     * @return newmap	the new node created
     */
    private   Map<String, Object> createNode(Map<String, Object> current,
            String in) {
        Map<String, Object> newMap = new LinkedHashMap<String, Object>();
        newMap.put("name", in);

        if (current.containsKey("children")) {
            ((List<Map<String, Object>>) current.get("children")).add(newMap);
        } else {
            //add new subcategory if it does not already exist
            current.put("children", new ArrayList<Object>());
            ((List<Map<String, Object>>) current.get("children")).add(newMap);
        }

        return newMap;
    }

    /**
     * Sequentially searches through the current map for input string.
     *
     * @param current the current map to search through
     * @param in the search string
     * @return the node requested if found; null otherwise
     */
    private Map<String, Object> findName(Map<String, Object> current,
            String in) {
        if (current.containsKey("children")) {
            List<Object> temp = ((List<Object>) current.get("children"));

            for (int i = 0; i < temp.size(); i++) {
                if (((Map<Object, Object>) temp.get(i)).get("name").equals(in)) {
                    return ((Map<String, Object>) temp.get(i));
                }
            }
        }
        return null;
    }
}
