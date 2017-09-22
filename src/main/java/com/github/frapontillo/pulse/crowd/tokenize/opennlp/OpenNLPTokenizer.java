package com.github.frapontillo.pulse.crowd.tokenize.opennlp;

import com.github.frapontillo.pulse.crowd.data.entity.Message;
import com.github.frapontillo.pulse.crowd.data.entity.Token;
import com.github.frapontillo.pulse.crowd.tokenize.ITokenizer;
import com.github.frapontillo.pulse.util.PulseLogger;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Francesco Pontillo
 */
public class OpenNLPTokenizer extends ITokenizer {
    public final static String PLUGIN_NAME = "tokenizer-opennlp";
    private Logger logger = PulseLogger.getLogger(OpenNLPTokenizer.class);
    private Map<String, Tokenizer> tokenizers;
    private Set<String> unhandledLangs;

    public OpenNLPTokenizer() {
        tokenizers = new HashMap<>();
        unhandledLangs = new HashSet<>();
    }

    @Override public String getName() {
        return PLUGIN_NAME;
    }

    @Override public List<Token> getTokens(Message message) {
        Tokenizer tokenizer = getTokenizer(message.getLanguage());
        if (tokenizer == null) {
            return null;
        }
        List<String> tokenList = Arrays.asList(tokenizer.tokenize(message.getText()));
        return tokenList.stream().map(Token::new).collect(Collectors.toList());
    }

    private Tokenizer getTokenizer(String language) {
        TokenizerModel model;
        Tokenizer tokenizer;
        // if the language could not be loaded before, don't try again
        if (unhandledLangs.contains(language)) {
            return null;
        }
        // attempt to load the tokenizer
        tokenizer = tokenizers.get(language);
        // if the tokenizer wasn't loaded before, load it
        if (tokenizer == null) {
            InputStream modelIn = null;
            try {
                // read the model and build the tokenizer
                modelIn = getClass().getClassLoader().getResourceAsStream(language + "-token.bin");
                model = new TokenizerModel(modelIn);
                tokenizer = new TokenizerME(model);
                tokenizers.put(language, tokenizer);
            } catch (IOException | IllegalArgumentException e) {
                unhandledLangs.add(language);
                logger.warn(String.format("There is no model for the language %s.", language));
            } finally {
                if (modelIn != null) {
                    try {
                        modelIn.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return tokenizer;
    }
}