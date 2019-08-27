package densityhtmlparser;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringEscapeUtils;

public class DensityHtml {
	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {

		// 输入HTML字符串
		StringBuilder buffer = new StringBuilder();
		InputStream is = null;
		try {
			is = new FileInputStream("C:/Users/Administrator/Desktop/test.txt");
		} catch (FileNotFoundException e1) {
			// TODO 自动生成的 catch 块
			e1.printStackTrace();
		}
		String line = null; // 用来保存每行读取的内容
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		try {
			while ((line = reader.readLine()) != null) {
				buffer.append(line + "\n");
			}
		} catch (IOException e2) {
			e2.printStackTrace();
		}

		String SaveTxt = null;
		SaveTxt = buffer.toString();

//		//字符串分割
//		String newContent = SaveTxt.substring(1,SaveTxt.length());
//		String[] strs = newContent.split("第");
		
		FileWriter fw = new FileWriter("C:/Users/Administrator/Desktop/111.txt");

		//循环里判断，一次输出N条数据
//		for(int i=0;i<30;i++){
			DensityHtml dh = new DensityHtml();
			Html2Article h2a = dh.new Html2Article();
//			Article content2 = h2a.GetArticle(strs[i]);
//			String title = h2a.GetTitle(strs[i]);
  		    Article content2 = h2a.GetArticle(SaveTxt);
		    String title = h2a.GetTitle(SaveTxt);
//			fw.write("第"+(i+1)+"个："+"标题如下：" + title + "\n网页正文如下：\n" + content2.Content+"\n\n");
		    fw.write("标题如下：" + title + "\n网页正文如下：\n" + content2.Content+"\n");
//		}
		fw.close();
		System.out.println(" ！");
	}

	// / 文章正文数据模型
	public class Article {
		// / 文章标题
		public String Title;
		// / 正文文本
		public String Content;
	}

	// / 解析Html页面的文章正文内容,基于文本密度的HTML正文提取类
	// / Date: 2012/12/30
	// / Update:
	// / 2013/7/10 优化文章头部分析算法，优化
	// / 2014/4/25 添加Html代码中注释过滤的正则
	public class Html2Article extends Article {
		// 正则表达式过滤：正则表达式，要替换成的文本
		private String[][] filters = {
				new String[] { "(?is)<script.*?>.*?</script>", "" },
				new String[] { "(?is)<style.*?>.*?</style>", "" },
				new String[] { "(?is)<!--.*?-->", "" }, // 过滤Html代码中的注释nnn
				// 针对链接密集型的网站的处理，主要是门户类的网站，降低链接干扰
				new String[] { "(?is)<ul.*?>.*?</ul>", "" } ,
				new String[] { "(?is)<a.*?>.*?</a>", "" } };
				//new String[] { "(?is)<a>", "</a>\n" } };

		private boolean _appendMode = false;

		// / 按行分析的深度，默认为6
		private int _depth = 6;

		// / 字符限定数，当分析的文本数量达到限定数则认为进入正文内容
		// / 默认180个字符数
		private int _limitCount = 180;

		// 确定文章正文头部时，向上查找，连续的空行到达_headEmptyLines，则停止查找
		private int _headEmptyLines = 2;
		// 用于确定文章结束的字符数
		private int _endLimitCharCount = 10;

		// / 从给定的Html原始文本中获取正文信息
		public Article GetArticle(String html) {
			// 如果换行符的数量小于10，则认为html为压缩后的html
			// 由于处理算法是按照行进行处理，需要为html标签添加换行符，便于处理
			int num = 0;

			// 循环遍历每个字符，判断是否是字符 a ，如果是，累加次数
			for (int i = 0; i < html.length(); i++) {
				// 获取每个字符，判断是否是字符a
				if (html.charAt(i) == '\n') {
					// 累加统计次数
					num++;
				}
			}

			if (num < 10) {
				html = html.replace(">", ">\n");
			}

			// 获取html，body标签内容
			String body = "";
			String bodyFilter = "(?is)<body.*?</body>";
			Pattern r = Pattern.compile(bodyFilter);
			Matcher m = r.matcher(html);
			if (m.find()) {
				body = m.group(0).toString();
			}
			// 过滤样式，脚本等不相干标签
			for (int i = 0; i < filters.length; i++) {
				body = body.replace(filters[i][0], filters[i][1]);
			}
			// 标签规整化处理，将标签属性格式化处理到同一行
			// 处理形如以下的标签：
			// <a
			// href='http://www.baidu.com'
			// class='test'
			// 处理后为
			// <a href='http://www.baidu.com' class='test'>
			body = body.replace("(<[^<>]+)\\s*\n\\s*", FormatTag(body));

			Article article = new Article();
			article = GetContent(body, article);

			return article;
		}

		// / 格式化标签，剔除匹配标签中的回车符
		// / <param name="match"></param>
		// / <returns></returns>
		private String FormatTag(String match) {
			StringBuilder sb = new StringBuilder();

			for (int i = 0; i < match.length(); i++) {
				// 获取每个字符，判断是否是字符a
				if (match.charAt(i) == '\r' || match.charAt(i) == '\n') {
					continue;
				}
				sb.append(match.charAt(i));
			}
			return sb.toString();
		}

		public String GetTitle(String html) {
			String titleFilter = "<title>[\\s\\S]*?</title>";
			String h1Filter = "<h1.*?>.*?</h1>";
			String clearFilter = "<.*?>";

			String title = "";
			Pattern t = Pattern.compile(titleFilter);
			Matcher m = t.matcher(html);
			if (m.find()) {
				Pattern p1 = Pattern.compile(clearFilter);
				Matcher m1 = p1.matcher(m.group(0).toString());
				title = m1.replaceAll("");
			}

			// 正文的标题一般在h1中，比title中的标题更干净
			Pattern t2 = Pattern.compile(h1Filter);
			Matcher m2 = t2.matcher(html);
			if (m2.find()) {
				Pattern p1 = Pattern.compile(clearFilter);
				Matcher m3 = p1.matcher(m2.group(0).toString());
				String h1 = m3.replaceAll("");
				if (!(h1 == null) && title.startsWith(h1)) {
					title = h1;
				}
			}

			return title;
		}

		// / 从body标签文本中分析正文内容
		// / <param name="bodyText">只过滤了script和style标签的body文本内容</param>
		// / <param name="content">返回文本正文，不包含标签</param>
		// / <param name="contentWithTags">返回文本正文包含标签</param>
		public Article GetContent(String bodyText, Article article) {
			String[] orgLines = null; // 保存原始内容，按行存储
			String[] lines = null; // 保存干净的文本内容，不包含标签

			orgLines = bodyText.split("\n");
			lines = new String[orgLines.length];
			// 去除每行的空白字符,剔除标签
			for (int i = 0; i < orgLines.length; i++) {
				String lineInfo = orgLines[i];
				// 处理回车，使用crlf做为回车标记符，最后统一处理
				// lineInfo = Regex.Replace(lineInfo, "(?is)</p>|<br.*?/>",
				// "crlf");
				String re1 = "(?is)</p>|<br.*?/>";
				Pattern p1 = Pattern.compile(re1);
				Matcher m1 = p1.matcher(lineInfo);
				lineInfo = m1.replaceAll("crlf");

				// lines[i] = Regex.Replace(lineInfo, "(?is)<.*?>", "").Trim();
				String re2 = "(?is)<.*?>";
				Pattern p2 = Pattern.compile(re2);
				Matcher m2 = p2.matcher(lineInfo);
				lines[i] = m2.replaceAll("").trim();
			}

			StringBuilder sb = new StringBuilder();
			StringBuilder orgSb = new StringBuilder();

			int preTextLen = 0; // 记录上一次统计的字符数量
			int startPos = -1; // 记录文章正文的起始位置
			for (int i = 0; i < lines.length - _depth; i++) {
				//记录连续6行的总字符数
				int len = 0;
				for (int j = 0; j < _depth; j++) {
					len += lines[i + j].length();
				}

				if (startPos == -1) // 还没有找到文章起始位置，需要判断起始位置
				{
					if (preTextLen > _limitCount && len > 0 && lines[i]!=null) // 如果上次查找的文本数量超过了限定字数，且当前行数字符数不为0，则认为是开始位置
					{
						// 查找文章起始位置, 如果向上查找，发现2行连续的空行则认为是头部
						int emptyCount = 0;
						for (int j = i - 1; j > 0; j--) {
							if (lines[j] == null) {
								emptyCount++;
							} else {
								emptyCount = 0;
							}
							if (emptyCount == _headEmptyLines) {
								startPos = j + _headEmptyLines;
								break;
							}
						}
						// 如果没有定位到文章头，则以当前查找位置作为文章头
						if (startPos == -1) {
							startPos = i;
						}
						// 填充发现的文章起始部分
						for (int j = startPos; j <= i; j++) {
							sb.append(lines[j]);
							orgSb.append(orgLines[j]);
						}
					}
				} else {
					// if (len == 0 && preTextLen == 0) //
					// 当前行长度小于10，且上一行长度也小于10，则认为已经结束
					if (len <= _endLimitCharCount&& preTextLen < _endLimitCharCount) {
						if (!_appendMode) {
							//文章结束，跳出
							break;
						}
						startPos = -1;
					}
					sb.append(lines[i]);
					orgSb.append(orgLines[i]);
				}
				preTextLen = len;
			}
			String result = sb.toString();
			// 处理回车符，更好的将文本格式化输出
			article.Content = result.replaceAll("crlf", "\r\n");
			article.Content = StringEscapeUtils.unescapeHtml(article.Content);

			return article;
		}
	}
}
