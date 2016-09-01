/******************************************************************************
 * Copyright 2013-2016 LASIGE                                                  *
 *                                                                             *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may     *
 * not use this file except in compliance with the License. You may obtain a   *
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0           *
 *                                                                             *
 * Unless required by applicable law or agreed to in writing, software         *
 * distributed under the License is distributed on an "AS IS" BASIS,           *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.    *
 * See the License for the specific language governing permissions and         *
 * limitations under the License.                                              *
 *                                                                             *
 *******************************************************************************
 * Test-runs AgreementMakerLight in Eclipse.                                   *
 *                                                                             *
 * @author Daniel Faria                                                        *
 ******************************************************************************/
package aml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import aml.match.*;
import aml.filter.Selector;
import aml.settings.EntityType;
import aml.settings.SelectionType;
import aml.util.ISub;
import aml.util.StopList;
import aml.util.StringParser;
import org.jfree.ui.Align;

public class MyMatcher extends AbstractInstanceMatcher {
    //Attributes
    private Set<String> stopSet;

    //Constructors
    public MyMatcher() {
        this(true);
    }

    public MyMatcher(boolean useWordNet) {
        super(useWordNet);
        stopSet = StopList.read();
    }

    //Public Methods
    public Alignment match(EntityType e, double threshold) throws UnsupportedEntityTypeException {
        if (!e.equals(EntityType.INDIVIDUAL))
            throw new UnsupportedEntityTypeException(e.toString());
        int classId = AML.getInstance().getSource().getLexicon().getBestEntity(EntityType.CLASS, "topic", true);


        Alignment a = new Alignment();

        //For each combination of instances, perform a name match
        for (Integer s : sourceInd) {
            if (!rels.belongsToClass(s, classId))
                continue;
            for (Integer t : targetInd) {
                double sim = nameSimilarity(s, t, true);
                if (sim >= threshold) {
                    a.add(s, t, sim);
                }
            }
        }

        //Perform selection
        Selector s = new Selector(threshold, SelectionType.PERMISSIVE);
        a = s.filter(a);

        return a;
    }

    //Private Methods
    protected double nameSimilarity(int i1, int i2, boolean useWordNet) {


        HashMap<String, Double> similarityMeasures = new HashMap<>();

        double names_Similarity = 0.0;
//        double synonym_Similarity = 0.0;
//        double acronym_Similarity = 0.0;
//        double multiword_Similarity = 0.0;
//        double commonWordsInArticles_similarity = 0.0;
//        double partOfSPeech_similarity = 0.0;
//        double definition_similarity = 0.0;
//        double homonym_matcher = 0.0;//
//        double similarTo_similarity = 0.0; //using this API https://www.wordsapi.com/docs#details
//
//        double cosine_Similarity = 0.0;
        /*
        * More similarities to come here
        * https://github.com/tdebatty/java-string-similarity
        * */

        /*
        *
        * Check if two strings are location and calculate location similarity.
        * // country similar, distance similar
        *
        *
        * */


        for (String n1 : sLex.getNames(i1)) {
            if (n1.length() != 0 && n1 != null) {
                for (String n2 : tLex.getNames(i2)) {
                    if (n2.length() != 0 && n2 != null) {


                        names_Similarity = Math.max(names_Similarity, nameSimilarity(n1, n2, useWordNet));
//                        acronym_Similarity = Math.max(acronym_Similarity, acronymMatcher(n1, n2));


                    }
                }
            }
        }

//        similarityMeasures.put("NamesSimilarity", names_Similarity);
//        similarityMeasures.put("synonymSimilarity", synonym_Similarity);
//        similarityMeasures.put("acronymSimilarity", acronym_Similarity);
//        similarityMeasures.put("multiwordSimilarity", multiword_Similarity);
//        similarityMeasures.put("commonWordsSimilarity", commonWordsInArticles_similarity);

        // change this return value after combining
        return names_Similarity;
    }


    protected double multiword_similarity() {
        return 0.0;
    }

    protected double acronymMatcher(String n1, String n2) {

        double acronymsimilarity = 0.0;
        String[] srcWords = n1.toLowerCase().split(" ");

        String[] tgtWords = n2.toLowerCase().split(" ");

        ArrayList<String> longer = new ArrayList<String>();
        ArrayList<String> shorter = new ArrayList<String>();
        if (srcWords.length == tgtWords.length)
            return 0.0;
        boolean sourceIsLonger = srcWords.length > tgtWords.length;
        if (sourceIsLonger) {
            for (String word : srcWords)
                longer.add(word);
            for (String word : tgtWords)
                shorter.add(word);
        } else {
            for (String word : srcWords)
                shorter.add(word);
            for (String word : tgtWords)
                longer.add(word);
        }
        int total = longer.size();
        //Check if they have shared words, and remove them
        for (int i = 0; i < shorter.size(); i++) {
            String word = shorter.get(i);
            if (longer.remove(word)) {
                shorter.remove(i--);
                acronymsimilarity += 1.0;
            }
        }
        if (shorter.size() != 1)
            return 0.0;


        String acronym = shorter.get(0);


        if (acronym.length() < 2 || acronym.length() > 3 || acronym.length() != longer.size())
            return 0.0;


        boolean match = true;
        for (int i = 0; i < longer.size(); i++) {
            String word = longer.get(i);
            match = word.startsWith(acronym.substring(i, i + 1));
            if (match)
                acronymsimilarity += 0.5;
            else
                break;
        }
        if (!match)
            return 0.0;
        acronymsimilarity /= total;

        return acronymsimilarity;

    }

    protected double nameSimilarity(String n1, String n2, boolean useWordNet) {
        //Check whether the names are equal
        if (n1.equals(n2))
            return 1.0;

        //Since we cannot use string or word similarity on formulas
        //if the names are (non-equal) formulas their similarity is zero
        if (StringParser.isFormula(n1) || StringParser.isFormula(n2))
            return 0.0;

        //Compute the String similarity
        double stringSim = ISub.stringSimilarity(n1, n2);

        //Compute the String similarity after removing stop words
        String n1S = n1;
        String n2S = n2;
        for (String s : stopSet) {
            n1S = n1S.replace(s, "").trim();
            n2S = n2S.replace(s, "").trim();
        }
        stringSim = Math.max(stringSim, ISub.stringSimilarity(n1S, n2S) * 0.95);

        //Compute the Word similarity (ignoring stop words)

        double wordSim = 0.0;

        //Split the source name into words
        String[] sW = n1.split(" ");
        HashSet<String> n1Words = new HashSet<String>();
        for (String s : sW)
            if (!stopSet.contains(s))
                n1Words.add(s);

        String[] tW = n2.split(" ");
        HashSet<String> n2Words = new HashSet<String>();
        for (String s : tW)
            if (!stopSet.contains(s))
                n2Words.add(s);

        //Do a weighted Jaccard, where words are weighted by length
        double total = 0.0;
        for (String s : n1Words) {
            if (n2Words.contains(s))
                wordSim += s.length();
            else
                total += s.length();
        }
        for (String s : n2Words)
            total += s.length();
        wordSim /= total;


        return Math.max(stringSim, wordSim);
    }


    //Main Method
    public static void main(String[] args) throws Exception {
        //Path to input ontology files (edit manually)


        String sourcePath = "/Users/kshitijgautam/IdeaProjects/AML-Project/AgreementMakerLight/data/sabine_linking/sabine_source.owl";
        String targetPath = "/Users/kshitijgautam/IdeaProjects/AML-Project/AgreementMakerLight/data/sabine_linking/sabine_target.owl";
        String referencePath = "/Users/kshitijgautam/IdeaProjects/AML-Project/AgreementMakerLight/data/sabine_linking/refalign.rdf";

        //Path to save output alignment (edit manually, or leave blank for no evaluation)
        String outputPath = "";

        //Get AML instance and open ontologies
        long time = System.currentTimeMillis();
        AML aml = AML.getInstance();
        aml.openOntologies(sourcePath, targetPath);

        //Run instance matcher and save output alignment
        System.out.println("Running Instance Matcher");

        //Set threshold
        double threshold = 0.9;

        //Matching Algorithm
        MyMatcher mm = new MyMatcher();
        Alignment myMatcher = mm.match(EntityType.INDIVIDUAL, threshold);
//        Alignment multiWordMatcher = new MultiWordMatcher().match(EntityType.INDIVIDUAL, threshold);
//        Alignment thesaurusMatcher = new ThesaurusMatcher().match(EntityType.INDIVIDUAL, threshold);
//        Alignment logicaldefinitionMatcher = new LogicalDefMatcher().match(EntityType.CLASS, threshold);
//        Alignment background = new BackgroundKnowledgeMatcher().match(EntityType.CLASS, threshold);
        Alignment hybridString = new HybridStringMatcher(true).match(EntityType.INDIVIDUAL, threshold);
//        Alignment lexicalMatcher = new LexicalMatcher().match(EntityType.INDIVIDUAL, threshold);


//        Alignment spaceLexicalMatcher = new SpacelessLexicalMatcher().match(EntityType.INDIVIDUAL, threshold);

//        Alignment valueLexiconMatcher = new Value2LexiconMatcher(true).match(EntityType.INDIVIDUAL, threshold);
//        Alignment valueMatcher = new ValueMatcher().match(EntityType.INDIVIDUAL, threshold);
//        Alignment valueStringMatcher = new ValueStringMatcher().match(EntityType.INDIVIDUAL, threshold);
//        Alignment wordNetMatcher = new WordNetMatcher().match(EntityType.INDIVIDUAL, threshold);


        // combined Alginemnt of differnet matchers, need to run experiments too
//        Alignment combinedAlignment1, combinedAlignment2;
//        combinedAlignment1 = LWC.combine(myMatcher, lexicalMatcher, 0.9); //0.8 weight to myMatcher
//        combinedAlignment2 = LWC.combine(hybridString, spaceLexicalMatcher, 0.3);


        aml.setAlignment(LWC.combine(myMatcher, hybridString, 0.5));

        time = (System.currentTimeMillis() - time) / 1000;
        System.out.println("Finished in " + time + " seconds");

        //Open the reference alignment and evaluate
        aml.openReferenceAlignment(referencePath);
        Alignment ref = aml.getReferenceAlignment();
        aml.evaluate();
        System.out.println(aml.getEvaluation());

    }
}