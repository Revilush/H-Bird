// , wordRegExp: "([\\w]+|[,'\"\(\)]\\w?)"						///[\u2000-\u206F\u2E00-\u2E7F\\'!"#$%&()*+,\-.\/:;<=>?@\[\]^_`{|}~]/

function lcsDiff(options) {
	var defaltOptions = {
		ignoreCase:false
		, ignorePunctuations:true
		, suffixDiffWordsWhileMerging : ""			// default is empty to join/merge individual diff-words into snippets
		
	};
		
	var _zero = 0;
	var _up = 1;
	var _left = 2;
	var _diagonal = 3;
	
	var synIdHasPunct_Regex = /__sy_[\d_]+/g;
	var wordHasPunct_Regex = /^[^\w]+[\w]*[^\w]*$|^\w+[^\w ]+[\w ]*$|__sy_[\d_]+[^\w ]+[\w ]*$/g;
	var textStartsWithPunct_Regex = /^[^\w]+/g;
	var textStartsWithWord_Regex = /^[\w ]+/g;

	
	
	
	var punctWordRegex = new RegExp("^[^\\w]+\\w*$|^\\w+[^\\w]+\\w*$", "g");
	
	this.lcsText = null;
    var config = deepCopy(options, defaltOptions);
	
	this.getDiffJson = function(text1, text2) {
		var lcsDif = getLongestCommonSequence_texts(text1, text2);
		this.lcsText = lcsDif.lcs;
		lcsDif = lcsDif.diff;
		mergeAdjacentDiffs(lcsDif);
		//console.log("post merge: ", lcsDif.length, lcsDif);
		return lcsDif;
	}
	this.getDiffJsonByWords = function(words1, words2) {
		var lcsDif = getLongestCommonSequence(words1, words2);
		this.lcsText = lcsDif.lcs;
		lcsDif = lcsDif.diff;
		mergeAdjacentDiffs(lcsDif);
		//console.log("post merge: ", lcsDif.length, lcsDif);
		return lcsDif;
	}

	this.getDiffHtml = function(text1, text2) {
		var lcsDif = this.getDiffJson(text1, text2);
		return this.getDiffHtmlByJson(lcsDif);
	}

	this.getDiffHtmlByJson = function(lcsDif) {
		var differ = this;
		
		function getDiffText(dif) {
			if (dif[0] == "+") {
				return differ.getDiffInsertionHtml(dif[2]);
			} else if (dif[0] == "-") {
				return differ.getDiffDeletionHtml(dif[2]);
			} else {
				// its equal
				return dif[2];
			}
		}
		
		//var wordStart = /^[\w]+/g;
		var dif, html = "<span>";
		html += getDiffText(lcsDif[0]);		// add first diff htm
		for (var i=1; i < lcsDif.length; i++) {
			dif = lcsDif[i];
			//if (dif[2].match(wordStart))
			//	html += " ";
			html += getDiffText(dif);
		}
		html += "</span>";
		
		return html;
	}
	
	this.getCSS = function() {
		var css = '.wikEdDiffInsert , .wikEdDiffInsertBlank {' +
			'text-decoration:underline;' +
			'color: #E11; border-radius: 0.25em; padding: 0.2em 1px; ' +
		'} ' +
		// Delete
		'.wikEdDiffDelete , .wikEdDiffDeleteBlank{' +
			'color: #F00 !important; text-decoration:line-through !important; ' +
			'color: #222; border-radius: 0.25em; padding: 0.2em 1px; ' +
		'} '+
		'.wikEdDiffSpace { position: relative; } ' +
		'.wikEdDiffSpaceSymbol { position: absolute; top: -0.2em; left: -0.05em; } ' +
		'.wikEdDiffSpaceSymbol:before { content: " "; color: transparent; } '
		;
		
		return css;
	}
	
	this.countDiffWordsByJson = function(diffs) {
		var dc = 0;
		if (!diffs  ||  diffs.length == 0)
			return dc;
		var words;
		diffs.forEach(function(idx, dif){
			if (dif[0] != "=") {
				words = dif[2].split(/[\s]+/g).filter(function(n) {return n});
				dc += words.length;
			}
		})
		return dc;
	}
	this.countDiffLettersByJson = function(diffs) {
		var dc = 0;
		if (!diffs  ||  diffs.length == 0)
			return dc;
		diffs.forEach(function(idx, dif){
			if (dif[0] != "=") {
				dc += dif[2].length;
			}
		})
		return dc;
	}
	
	this.countDiffWords = function(diffHtml) {
		var diffEle = diffHtml;
		var diffs = diffEle.find(".wikEdDiffInsert, .wikEdDiffDelete");
		var diffWords = 0;
		if (diffs.length > 0) {
			var texts, words;
			diffs.forEach(function(idx, diff){
				texts = diff.text();
				words = texts.split(/[\s]+/g).filter(function(n) {return n});		// filter will remove empty splits' parts
				diffWords += words.length;
			})
		}
		return diffWords;
	}
	this.countDiffLetters = function(diffHtml) {
		var diffEle = diffHtml;
		var diffs = diffEle.find(".wikEdDiffInsert, .wikEdDiffDelete");
		var diffWords = 0;
		if (diffs.length > 0) {
			var texts, words;
			diffs.forEach(function(idx, diff){
				diffWords += diff.text().length;
			})
		}
		return diffWords;
	}
	
	
	// matrix/grid holder
	function table(height, width) {
		var table = new Array(height);
		for (var i = 0; i < height; i++)
			table[i] = new Array(width);
		return table;
	}
	
	function splitText2WordsArray(text) {
		//const regexpCurrencyOrPunctuation = /\p{Sc}|\p{P}/gu;
		//console.log(sentence.match(regexpCurrencyOrPunctuation)); 	expected output: Array ["¥", "."]
		
		/*
		 * split by space but keep the space suffixed to previous word. We'll need to ignore space (ie trim) before matching words.
		 */
		//var words = text.split(/(\S+\s+)/).filter(function(n) {return n});			//.split(" ");
		
		///If you wrap the delimiter in parantheses it will be part of the returned array 
		///(https://stackoverflow.com/questions/12001953/javascript-and-regex-split-string-and-keep-the-separator)
		// split so that words and space are separated, but keep spaces as well.
		var words = text.split(/( )/g);
		
		if (config.ignorePunctuations) {
			// we need to identify words with punctuations, ie cases like:   "Trustee" / Trustee's / which, / offer), / (in /
			// once found, we need to split punct from words
			var parts;
			for (var i=0; i < words.length; i++) {
				// if word has puncts, split and keep all separately
				if (words[i].match(wordHasPunct_Regex)) {
					// we have a word that contain some puncts.. split them and insert 2+ parts into words array
					parts = splitWordAndPunctuation(words[i]);
					words.splice(i, 1);
					parts.forEach( function(idx, p){
						words.splice(i+idx, 0, p);
					})
					i += parts.length -1;
				}
			}
		}
		return words;
	}
	
	function splitWordAndPunctuation(word) {
		textStartsWithWord_Regex.lastIndex=0;
		synIdHasPunct_Regex.lastIndex=0;
		textStartsWithPunct_Regex.lastIndex=0;
		
		var words = [];
		var match = textStartsWithWord_Regex.exec(word);
		if (null != match  &&  match.length > 0) {
			// starts with word - we have puncts at the end
			words[0] = match[0];
			if (textStartsWithWord_Regex.lastIndex < word.length)
				words[1] = word.substring(textStartsWithWord_Regex.lastIndex);			//replace(words[0], "");
		} else {
			// we have puncts at the start
			match = synIdHasPunct_Regex.exec(word);
			if (null != match  &&  match.length > 0) {
				words[0] = match[0];
				if (synIdHasPunct_Regex.lastIndex < word.length)
					words[1] = word.substring(synIdHasPunct_Regex.lastIndex);
			} else {
				match = textStartsWithPunct_Regex.exec(word);
				if (null != match  &&  match.length > 0) {
					words[0] = match[0];
					// the word may also end with punct - recurse to find out and append to list
					var parts = splitWordAndPunctuation(word.substring(textStartsWithPunct_Regex.lastIndex));
					for (var i=0; i < parts.length; i++)
						words[words.length] = parts[i];
					//words[1] = word.substring(textStartsWithPunct_Regex.lastIndex);			//words[1] = word.replace(words[0], "");
				}
			}
		}
		if (words.length == 0)
			return [word];
		return words;
	}

	function getLongestCommonSequence_texts(text1, text2) {
		// split strings by space/tab etc - make array of words
		var a = splitText2WordsArray(text1);		//.split(config.wordRegex);
		var b = splitText2WordsArray(text2);		//.split(config.wordRegex);
		// return diff by array
		return getLongestCommonSequence(a, b);
	}
		
	function getLongestCommonSequence(a, b) {
		var lcs = table(a.length + 1, b.length + 1);
		var directions = table(a.length + 1, b.length + 1);

		// a _zero
		for (var i = 0; i <= b.length; i++) {
			lcs[0][i] = 0;
			directions[0][i] = _zero;
		}

		// b _zero
		for (var i = 0; i <= a.length; i++) {
			lcs[i][0] = 0;
			directions[i][0] = _zero;
		}
		
		function wordsMatch(w1, w2) {
			var s1 = w1, s2 = w2;
			
			if (config.ignoreCase) {
				return (s1.toLowerCase() === s2.toLowerCase())
			} else 
				return (s1 === s2);
		}
		
		for (var i = 1; i <= a.length; i++) {
			for (var j = 1; j <= b.length; j++) {
				if ( wordsMatch(a[i - 1] , b[j - 1]) ) {
					lcs[i][j] = lcs[i - 1][j - 1] + 1;
					directions[i][j] = _diagonal;
				} else {
					var upLcs = lcs[i - 1][j];
					var leftLcs = lcs[i][j - 1];
					if (upLcs >= leftLcs) {
						lcs[i][j] = upLcs;
						directions[i][j] = _up;
					} else {
						lcs[i][j] = leftLcs;
						directions[i][j] = _left;
					}
				}
			}
		}

		var result = '';
		var diff = [];
		var i = a.length, j = b.length;
		var m = directions[i][j];
		while (m !== _zero) {
			if (m === _diagonal) {
				i--;
				j--;
				result = a[i] +" "+ result;
				diff.push(['=', i, a[i]]);
			} else if (m === _left) {
				j--;
				diff.push(['+', j, b[j]]);
			} else if (m === _up) {
				i--;
				diff.push(['-', i, a[i]]);
		    }
			m = directions[i][j];
		}
		while (j > 0) {
			j--;
			diff.push(['+', j, b[j]]);
		}
		while (i > 0) {
			i--;
			diff.push(['-', i, a[i]]);
		}
		diff.reverse()
		
		return { lcs: result, 'diff':diff }
	}

	/**
	 * Merges adjacent words if their diff token-type (=/-/+) is same.
	 */
	function mergeAdjacentDiffs(diffs) {
		//var punctStart = /^[^\w]+/;
		
		var dif, prvDif;
		for (var i=1; i < diffs.length; i++) {
			prvDif = diffs[i-1];
			dif = diffs[i];
			// see if this dif's op is same as previous one's
			if (dif[0] != prvDif[0])
				continue;
			// merge the diffs
			prvDif[2] += config.suffixDiffWordsWhileMerging + dif[2];
			diffs.splice(i, 1);
			i--;
		}
	}
	
	
	/**
	 * Recursive deep copy from target over source for customization of configs.
	 *
	 * @param object source Source object
	 * @param object target Target object
	 */
	function deepCopy( source, target ) {
		target = target || {};
		target = JSON.parse(JSON.stringify(target));		// make a copy
		for ( var key in source ) {
			if ( Object.prototype.hasOwnProperty.call( source, key ) === true ) {
				if ( typeof source[key] === 'object' ) {
					deepCopy( source[key], target[key] );
				}
				else {
					target[key] = source[key];
				}
			}
		}
		return target;
	};

	this.getDiffSpaceHtml = function() {
		return '<span class="wikEdDiffSpace"><span class="wikEdDiffSpaceSymbol"></span> </span>';
	}
	this.getDiffDeletionHtml = function(text){
		//<span class="wikEdDiffDelete" title="-">word1<span class="wikEdDiffSpace"><span class="wikEdDiffSpaceSymbol"></span></span>word2.......</span>
		words = text.split(" ");
		var html = '<span class="wikEdDiffDelete" title="-">' + words.join(this.getDiffSpaceHtml()) + "</span>";
		return html;
	}
	this.getDiffInsertionHtml = function(text){
		//<span class="wikEdDiffInsert" title="+">word1<span class="wikEdDiffSpace"><span class="wikEdDiffSpaceSymbol"></span></span>word2......</span>
		words = text.split(" ");
		var html = '<span class="wikEdDiffInsert" title="+">' + words.join(this.getDiffSpaceHtml()) + "</span>";
		return html;
	}

	
}
	

