package aml;


import aml.knowledge.WordNet;

/*
* Testing wordnet API
* https://www.experts-exchange.com/questions/24512461/Java-API-for-WordNet-Searching.html
* http://stackoverflow.com/questions/5976537/wordnet-similarity-in-java-jaws-jwnl-or-java-wnsimilarity
* */
public class TestingWordnet {


    public static void main(String[] args) {
        WordNet wordnet = new WordNet();
        System.out.println(wordnet.getHypernyms("location"));
    }


}