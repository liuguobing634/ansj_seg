package org.ansj.lucene5;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ansj.library.AmbiguityLibrary;
import org.ansj.library.CrfLibrary;
import org.ansj.library.DicLibrary;
import org.ansj.library.FilterLibrary;
import org.ansj.library.SynonymsLibrary;
import org.ansj.lucene.util.AnsjTokenizer;
import org.ansj.recognition.impl.FilterRecognition;
import org.ansj.recognition.impl.SynonymsRecgnition;
import org.ansj.splitWord.Analysis;
import org.ansj.splitWord.analysis.BaseAnalysis;
import org.ansj.splitWord.analysis.DicAnalysis;
import org.ansj.splitWord.analysis.IndexAnalysis;
import org.ansj.splitWord.analysis.NlpAnalysis;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.nlpcn.commons.lang.tire.domain.Forest;
import org.nlpcn.commons.lang.tire.domain.SmartForest;
import org.nlpcn.commons.lang.util.StringUtil;
import org.nlpcn.commons.lang.util.logging.Log;
import org.nlpcn.commons.lang.util.logging.LogFactory;

public class AnsjAnalyzer extends Analyzer {
	public final Log logger = LogFactory.getLog();

	/**
	 * dic equals user , query equals to
	 * 
	 * @author ansj
	 *
	 */
	public static enum TYPE {
		base, index, query, to, dic, user, search, nlp
	}

	/**
	 * 分词类型
	 */
	private Map<String, String> args;

	/**
	 * @param filter 停用词
	 */
	public AnsjAnalyzer(Map<String, String> args) {
		this.args = args;
	}

	public AnsjAnalyzer(TYPE type, String dics) {
		this.args = new HashMap<String, String>();
		args.put("type", type.name());
		args.put(DicLibrary.DEFAULT, dics);
	}

	public AnsjAnalyzer(TYPE type) {
		this.args = new HashMap<String, String>();
		args.put("type", type.name());
	}

	@Override
	protected TokenStreamComponents createComponents(String text) {
		BufferedReader reader = new BufferedReader(new StringReader(text));
		Tokenizer tokenizer = null;
		tokenizer = getTokenizer(reader, this.args);
		return new TokenStreamComponents(tokenizer);
	}

	/**
	 * 获得一个tokenizer
	 * 
	 * @param reader
	 * @param type
	 * @param filter
	 * @return
	 */
	public static Tokenizer getTokenizer(BufferedReader reader, Map<String, String> args) {

		Analysis analysis = null;

		String temp = null;

		switch (AnsjAnalyzer.TYPE.valueOf(args.get("type"))) {
		case base:
			analysis = new BaseAnalysis();
			break;
		case index:
			analysis = new IndexAnalysis();
			break;
		case dic:
		case user:
			analysis = new DicAnalysis();
			break;
		case to:
		case query:
		case search:
			analysis = new ToAnalysis();
			break;
		case nlp:
			analysis = new NlpAnalysis();
			if (StringUtil.isNotBlank(temp = args.get(CrfLibrary.DEFAULT))) {
				((NlpAnalysis) analysis).setCrfModel(CrfLibrary.get(temp));
			}
			break;
		default:
			analysis = new BaseAnalysis();
		}

		if (reader != null) {
			analysis.resetContent(reader);
		}

		if (StringUtil.isNotBlank(temp = args.get(DicLibrary.DEFAULT))) { //用户自定义词典
			String[] split = temp.split(",");
			Forest[] forests = new Forest[split.length];
			for (int i = 0; i < forests.length; i++) {
				if (StringUtil.isBlank(split[i])) {
					continue;
				}
				forests[i] = DicLibrary.get(split[i]);
			}
			analysis.setForests(forests);
		}

		List<FilterRecognition> filters = null;
		if (StringUtil.isNotBlank(temp = args.get(FilterLibrary.DEFAULT))) { //用户自定义词典
			String[] split = temp.split(",");
			filters = new ArrayList<FilterRecognition>();
			for (String key : split) {
				FilterRecognition filter = FilterLibrary.get(key.trim());
				if (filter != null)
					filters.add(filter);
			}
		}

		List<SynonymsRecgnition> synonyms = null;
		if (StringUtil.isNotBlank(temp = args.get(SynonymsLibrary.DEFAULT))) { //同义词词典
			String[] split = temp.split(",");
			synonyms = new ArrayList<SynonymsRecgnition>();
			for (String key : split) {
				SmartForest<List<String>> sf = SynonymsLibrary.get(key.trim());
				if (sf != null)
					synonyms.add(new SynonymsRecgnition(sf));
			}
		}

		if (StringUtil.isNotBlank(temp = args.get(AmbiguityLibrary.DEFAULT))) { //歧义词典
			analysis.setAmbiguityForest(AmbiguityLibrary.get(temp.trim()));
		}

		if (StringUtil.isNotBlank(temp = args.get("isNameRecognition"))) { // 是否开启人名识别
			analysis.setIsNameRecognition(Boolean.valueOf(temp));
		}

		if (StringUtil.isNotBlank(temp = args.get("isNumRecognition"))) { // 是否开启数字识别
			analysis.setIsNumRecognition(Boolean.valueOf(temp));
		}

		if (StringUtil.isNotBlank(temp = args.get("isQuantifierRecognition"))) { //量词识别
			analysis.setIsQuantifierRecognition(Boolean.valueOf(temp));
		}

		if (StringUtil.isNotBlank(temp = args.get("isRealName"))) { //是否保留原字符
			analysis.setIsRealName(Boolean.valueOf(temp));
		}

		return new AnsjTokenizer(analysis, filters, synonyms);

	}

}