package com.tc.poolparty.impl;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.tc.aem.importer.AEMPackageImporter;
import com.tc.poolparty.PoolPartyManager;
import com.tc.process.handler.TCTransformerHandler;

public class PoolPartyManagerImpl implements PoolPartyManager {

	private Properties poolPropertyProperties;
	static Logger LOG = Logger.getLogger(PoolPartyManagerImpl.class);

	public PoolPartyManagerImpl(Properties poolPropertyProperties) {
		this.poolPropertyProperties = poolPropertyProperties;
	}

	@Override
	public void createTags(List<PoolPartyBean> tags) {
		String organizationName = poolPropertyProperties
				.getProperty("poolparty.organizationName");
		String destinationMetaInfoDir = poolPropertyProperties
				.getProperty("poolparty.jcrRootParentDir");
		File metaInfDir = new File(destinationMetaInfoDir + File.separator
				+ "META-INF");
		String jcrRootParentDir = poolPropertyProperties
				.getProperty("poolparty.jcrRootParentDir");
		File directory = new File(jcrRootParentDir);
		if (!directory.exists()) {
			LOG.info("Parent folder, " + directory.getName()
					+ " Folder does not exists!");
			directory.mkdirs();
			LOG.info("created parent folder, " + directory.getName());
		}
		String tagsDirectory = null;
		File jcrRootDir = new File(directory.getAbsolutePath() + File.separator
				+ "jcr_root");
		if (jcrRootDir.exists()) {
			LOG.info(jcrRootDir.getName() + " already exists");
			try {
				FileUtils.deleteDirectory(jcrRootDir);
				LOG.info("So deleted " + jcrRootDir.getName());
			} catch (IOException e) {
				LOG.error(e);
			}
			tagsDirectory = createJCRRootDirStructure(jcrRootDir);
		} else {
			tagsDirectory = createJCRRootDirStructure(jcrRootDir);
		}

		String srcMetaInfoDir = poolPropertyProperties
				.getProperty("poolparty.metainffolder");
		createMetaInfoDir(srcMetaInfoDir, destinationMetaInfoDir);
		String destinationFolder = tagsDirectory + File.separator
				+ organizationName;
		File tagsFolder = new File(destinationFolder);
		if (!tagsFolder.exists()) {
			if (tagsFolder.mkdir()) {
				LOG.info(tagsFolder.getName() + " Folder is created!");
			} else {
				LOG.info("Failed to create " + tagsFolder.getName()
						+ " folder!");
			}
		}
		createParentTagContentXML(tags, destinationFolder, organizationName);

		for (PoolPartyBean tag : tags) {
			String xmlContent = createXMLContent(tag);
			createContentXML(tag, xmlContent, destinationFolder);
			List<PoolPartyBean> childTags = tag.getTags();
			for (PoolPartyBean bean : childTags) {
				xmlContent = createXMLContent(bean);
				createContentXML(bean, xmlContent, destinationFolder
						+ File.separator + tag.getKey());
				createChildXml(bean,
						destinationFolder + File.separator + tag.getKey());
			}

		}

		File jcrRootParentFolder = new File(jcrRootParentDir);
		FileOutputStream fos = null;
		ZipOutputStream zos = null;
		String aemPackagePath = jcrRootParentFolder.getAbsolutePath()
				+ File.separator + jcrRootParentFolder.getName() + ".zip";
		try {
			fos = new FileOutputStream(aemPackagePath);
			zos = new ZipOutputStream(fos);
			TCTransformerHandler tcNewLetterTransformerHandler = new TCTransformerHandler();
			tcNewLetterTransformerHandler.addDirToZipArchive(zos, jcrRootDir,
					null, false);
			tcNewLetterTransformerHandler.addDirToZipArchive(zos, metaInfDir,
					null, false);
		} catch (FileNotFoundException fileNotFoundException) {
			LOG.error(fileNotFoundException);
		} catch (IOException ioException) {
			LOG.error(ioException);
		} catch (Exception exception) {
			LOG.error(exception);
		} finally {
			try {
				zos.close();
			} catch (IOException e) {
				LOG.error(e);
			}
		}
		LOG.info("AEM Zip File is Created");
		AEMPackageImporter aemPackageImporter = new AEMPackageImporter();
		InputStream aemInputStream = this.getClass().getClassLoader()
				.getResourceAsStream("aem.properties");
		Properties aemProperties = new Properties();
		try {
			aemProperties.load(aemInputStream);
		} catch (IOException e) {
			LOG.error(e);
		}
		String repoURL = aemProperties.getProperty("aem.url") + "/crx/server";
		String aemUserName = aemProperties.getProperty("aem.userid");
		String aemPassword = aemProperties.getProperty("aem.password");

		/*
		 * boolean uninstallationStatus = aemPackageImporter.uninstallPackage(
		 * repoURL, aemUserName, aemPassword, aemPackagePath); if
		 * (uninstallationStatus) {
		 * LOG.info("Package uninstalled successfully..."); } else {
		 * LOG.info("Failed to uninstall the Package..."); }
		 */
		boolean installationStatus = aemPackageImporter.importPackage(repoURL,
				aemUserName, aemPassword, aemPackagePath, false);
		if (installationStatus) {
			LOG.info("Package installed successfully...");
		} else {
			LOG.info("Failed to install the package...");
		}
	}

	public void createChildXml(PoolPartyBean parentTag, String parentFolder) {

		if (parentTag.getTags() != null && parentTag.getTags().size() > 0) {
			for (PoolPartyBean tag : parentTag.getTags()) {
				String xmlContent = createXMLContent(tag);
				createContentXML(tag, xmlContent, parentFolder + File.separator
						+ parentTag.getKey());
				createChildXml(tag,
						parentFolder + File.separator + parentTag.getKey());
			}
		}
	}

	/**
	 * Creates the jcr root dir structure with .content.xml files.
	 * 
	 * @param jcrRootDir
	 *            the jcr root dir
	 * @return the string
	 */
	private String createJCRRootDirStructure(File jcrRootDir) {
		String tagsDirectory = null;
		if (jcrRootDir.mkdir()) {
			LOG.info(jcrRootDir.getName() + " is created");
		}
		String jcrRootXMLContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<jcr:root xmlns:sling=\"http://sling.apache.org/jcr/sling/1.0\" xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" xmlns:rep=\"internal\" jcr:mixinTypes=\"[rep:AccessControllable,rep:RepoAccessControllable]\" jcr:primaryType=\"rep:root\" sling:resourceType=\"sling:redirect\" sling:target=\"/index.html\"/>";
		createXMLFile(jcrRootXMLContent, jcrRootDir.getAbsolutePath()
				+ File.separator + ".content.xml");
		File etcDir = new File(jcrRootDir.getAbsolutePath() + File.separator
				+ "etc");
		if (etcDir.mkdir()) {
			LOG.info(etcDir.getName() + " is created");
		}
		String etcXMLContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<jcr:root xmlns:sling=\"http://sling.apache.org/jcr/sling/1.0\" xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" xmlns:rep=\"internal\" jcr:mixinTypes=\"[rep:AccessControllable]\" jcr:primaryType=\"sling:Folder\"/>";
		createXMLFile(etcXMLContent, etcDir.getAbsolutePath() + File.separator
				+ ".content.xml");
		File tagsDir = new File(etcDir.getAbsolutePath() + File.separator
				+ "tags");
		if (tagsDir.mkdir()) {
			LOG.info(tagsDir.getName() + " is created");
		}
		String tagsXMLContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<jcr:root xmlns:sling=\"http://sling.apache.org/jcr/sling/1.0\" xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" xmlns:rep=\"internal\" jcr:mixinTypes=\"[rep:AccessControllable,sling:Redirect]\" jcr:primaryType=\"sling:Folder\" jcr:title=\"Tags\" sling:resourceType=\"sling:redirect\" sling:target=\"/tagging\" hidden=\"{Boolean}true\" languages=\"[en,de,es,fr,it,pt_br,zh_cn,zh_tw,ja,ko_kr]\"/>";
		createXMLFile(tagsXMLContent, tagsDir.getAbsolutePath()
				+ File.separator + ".content.xml");
		tagsDirectory = tagsDir.getAbsolutePath();
		return tagsDirectory;
	}

	/**
	 * Creates the xml file with the given xml content under the given path.
	 * 
	 * @param xmlContent
	 *            the xml content
	 * @param path
	 *            the path
	 */
	private void createXMLFile(String xmlContent, String path) {
		FileWriter fw = null;
		try {
			File xmlFile = new File(path);
			if (!xmlFile.exists()) {
				fw = new java.io.FileWriter(path);
				fw.write(xmlContent);
			} else {
				LOG.info(xmlFile.getName() + " already exists");
			}
		} catch (IOException e) {
			LOG.error(e);
		} finally {
			try {
				fw.close();
			} catch (IOException e) {
				LOG.error(e);
			}
		}

	}

	/**
	 * Creates the meta info dir.
	 * 
	 * @param srcMetaInfoDir
	 *            the src meta info dir
	 * @param destinationMetaInfoDir
	 *            the destination meta info dir
	 */
	private void createMetaInfoDir(String srcMetaInfoDir,
			String destinationMetaInfoDir) {
		File metaInfoDir = new File(srcMetaInfoDir);
		File destinationMetaInfoDirectory = new File(destinationMetaInfoDir
				+ File.separator + "META-INF");
		if (metaInfoDir.exists()) {
			LOG.info("Source " + metaInfoDir.getName() + " is exists");
			if (!destinationMetaInfoDirectory.exists()) {
				try {
					FileUtils.copyDirectoryToDirectory(metaInfoDir, new File(
							destinationMetaInfoDir));
				} catch (IOException e) {
					LOG.error(e);
				}
			} else {
				LOG.info(destinationMetaInfoDirectory.getName()
						+ " is already exists");
				try {
					FileUtils.deleteDirectory(destinationMetaInfoDirectory);
					LOG.info("So deleted "
							+ destinationMetaInfoDirectory.getName());
					FileUtils.copyDirectoryToDirectory(metaInfoDir, new File(
							destinationMetaInfoDir));
					LOG.info("Again copied "
							+ destinationMetaInfoDirectory.getName());
				} catch (IOException e) {
					LOG.error(e);
				}
			}

		} else {
			LOG.info("Source " + metaInfoDir.getName() + " does not exists");
		}

	}

	/**
	 * Creates the parent tag content xml.
	 * 
	 * @param tags
	 *            the tags
	 * @param destinationFolder
	 *            the destination folder
	 * @param organizationName
	 *            the organization name
	 */
	private void createParentTagContentXML(List<PoolPartyBean> tags,
			String destinationFolder, String organizationName) {
		String listOfTags = "";
		for (PoolPartyBean tag : tags) {
			listOfTags += "<" + tag.getKey() + "/>\n";
		}
		String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<jcr:root xmlns:sling=\"http://sling.apache.org/jcr/sling/1.0\" xmlns:cq=\"http://www.day.com/jcr/cq/1.0\" xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" jcr:description=\"\" jcr:primaryType=\"cq:Tag\" jcr:title=\""
				+ organizationName
				+ "\" sling:resourceType=\"cq/tagging/components/tag\">\n"
				+ listOfTags + "</jcr:root>";
		FileWriter fw = null;
		try {
			File tagFolder = new File(destinationFolder);
			if (tagFolder.exists()) {
				fw = new java.io.FileWriter(destinationFolder + File.separator
						+ ".content.xml");
				fw.write(xmlContent);
				LOG.info("Created .content.xml file for the organization"
						+ organizationName);
			} else {
				LOG.info(tagFolder.getName() + " folder is not availabe");
			}
		} catch (IOException e) {
			LOG.error(e);
		} finally {
			try {
				fw.close();
			} catch (IOException e) {
				LOG.error(e);
			}
		}

	}

	/**
	 * Creates the xml content for the given tag.
	 * 
	 * @param tag
	 *            the tag
	 * @return the string
	 */
	private String createXMLContent(PoolPartyBean tag) {
		String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<jcr:root xmlns:sling=\"http://sling.apache.org/jcr/sling/1.0\" xmlns:cq=\"http://www.day.com/jcr/cq/1.0\" xmlns:jcr=\"http://www.jcp.org/jcr/1.0\" jcr:description=\"\" jcr:primaryType=\"cq:Tag\" jcr:title=\""
				+ tag.getKey()
				+ "\" sling:resourceType=\"cq/tagging/components/tag\"/>";
		return xmlContent;
	}

	/**
	 * Creates the content xml file for the given tag under the given folder
	 * with the given content.
	 * 
	 * @param tag
	 *            the tag
	 * @param xmlContent
	 *            the xml content
	 * @param destinationFolder
	 *            the destination folder
	 */
	private void createContentXML(PoolPartyBean tag, String xmlContent,
			String destinationFolder) {
		createTagFolder(tag, destinationFolder);
		FileWriter fw = null;
		try {
			File tagFolder = new File(destinationFolder + File.separator
					+ tag.getKey());
			if (tagFolder.exists()) {
				fw = new java.io.FileWriter(destinationFolder + File.separator
						+ tag.getKey() + File.separator + ".content.xml");
				fw.write(xmlContent);
			} else {
				LOG.info(tagFolder.getName() + " folder is not availabe");
			}
		} catch (IOException e) {
			LOG.error(e);
		} finally {
			try {
				fw.close();
			} catch (IOException e) {
				LOG.error(e);
			}
		}
	}

	/**
	 * Creates the tag folder.
	 * 
	 * @param tag
	 *            the tag
	 * @param destinationFolder
	 *            the destination folder
	 */
	private void createTagFolder(PoolPartyBean tag, String destinationFolder) {
		File tagFolder = new File(destinationFolder + File.separator
				+ tag.getKey());
		if (!tagFolder.exists()) {
			if (tagFolder.mkdir()) {
				LOG.info(tagFolder.getName() + " Folder is created!");
			} else {
				LOG.info("Failed to create " + tagFolder.getName() + " folder!");
			}
		} else {
			LOG.info(tagFolder.getName() + " is already exists");
		}
	}

	private JSONArray getNarrowers(String uri, boolean schemeFlag, String locale) {
		String masterJSON = getJSONFromPoolParty(uri, schemeFlag, locale);
		JSONArray bindings = null;
		JSONArray array = null;
		if (!StringUtils.isEmpty(masterJSON)) {

			bindings = new JSONArray(masterJSON);
			int length = bindings.length();
			for (int i = 0; i < length; i++) {
				JSONObject binding = bindings.getJSONObject(i);
				try {
					array = binding.getJSONArray("narrowers");
				} catch (JSONException e) {

				}

			}

		}

		return array;

	}

	private static int indent = 2;
	/*
	 * this property is for dev testing, when firstTime is true the recursive
	 * call will happen only for the first node.
	 */
	private boolean firstTime = false;

	@Override
	public List<PoolPartyBean> getTags(String concepts, boolean schemeFlag,
			String locale) {
		// LOG.info("Entered getTags()");

		List<PoolPartyBean> tags = null;
		String masterJSON = getJSONFromPoolParty(concepts, schemeFlag, locale);
		if (StringUtils.isEmpty(masterJSON)) {
			return tags;
		}

		try {
			JSONArray bindings = new JSONArray(masterJSON);
			tags = new ArrayList<PoolPartyBean>();
			LOG.debug(concepts + " has " + bindings.length());
			/*
			 * the length variable will be set 1 to test the function for only
			 * first node. make firstTime flag as false to run the recursive
			 * function for all tags
			 */
			int length = bindings.length();
			if (firstTime) {
				length = 1;
			}

			for (int i = 0; i < length; i++) {
				JSONArray narrowers = null;
				JSONObject binding = bindings.getJSONObject(i);
				String prefLabel = binding.getString("prefLabel");
				if (prefLabel != null) {
					/*
					 * some taxonomy labels contains characters which are not
					 * acceptable under a folder name, hence replacing them with
					 * valid chars
					 */
					prefLabel = prefLabel.replaceAll("/", "_");
					prefLabel = prefLabel.replaceAll("\\\\", "-");
					prefLabel = prefLabel.trim();
				}
				StringBuffer msg = new StringBuffer();
				for (int j = 0; j < indent; j++) {
					msg.append(" ");
				}
				String charSetPrefLabel = null;
				try {
					charSetPrefLabel = new String(prefLabel.getBytes(), "UTF-8");
				} catch (UnsupportedEncodingException e) {
					LOG.error(e);
					charSetPrefLabel = prefLabel;
				}

				msg.append(charSetPrefLabel);
				LOG.info(msg.toString());

				PoolPartyBean bean = new PoolPartyBean(charSetPrefLabel);

				List<String> childUris = new ArrayList<String>();
				try {
					narrowers = binding.getJSONArray("narrowers");
				} catch (JSONException je) {
					String uri = binding.getString("uri");
					narrowers = getNarrowers(uri, false, locale);
				}

				if (narrowers != null) {
					for (int j = 0; j < narrowers.length(); j++) {
						String narrower = narrowers.getString(j);
						childUris.add(narrower);
						indent += 2;
						//firstTime = false;
						List<PoolPartyBean> children = getTags(narrower, false,
								locale);
						indent -= 2;
						if (children != null) {
							bean.getTags().addAll(children);
						}

					}
				}
				bean.setNarrowers(childUris);
				tags.add(bean);
			}
		} catch (JSONException e) {
			LOG.error(e);
		}

		return tags;
	}

	/**
	 * Connects to PoolParty. Gets the jSON from pool party.
	 * 
	 * @return the jSON from pool party
	 */
	private String getJSONFromPoolParty(String topConcepts, boolean schemeFlag,
			String locale) {
		StringBuilder content = null;
		String serverAddress = poolPropertyProperties
				.getProperty("poolparty.serverAddress");
		String userId = poolPropertyProperties.getProperty("poolparty.userId");
		String password = poolPropertyProperties
				.getProperty("poolparty.password");
		boolean doOutput = Boolean.parseBoolean(poolPropertyProperties
				.getProperty("poolparty.doOutput"));
		String contentType = poolPropertyProperties
				.getProperty("poolparty.contentType");
		int connectTimeout = Integer.parseInt(poolPropertyProperties
				.getProperty("poolparty.connectTimeout"));
		int readTimeout = Integer.parseInt(poolPropertyProperties
				.getProperty("poolparty.readTimeout"));
		String encodedContentType = poolPropertyProperties
				.getProperty("poolparty.content-type");
		InputStream queryFileStream = this.getClass().getClassLoader()
				.getResourceAsStream("query.txt");
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				queryFileStream));
		StringBuilder query = new StringBuilder();
		String lineOfQuery;
		try {
			while ((lineOfQuery = reader.readLine()) != null) {
				query.append(lineOfQuery + "\n");
			}
		} catch (IOException e) {
			LOG.error(e);
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				LOG.error(e);
			}
		}
		BufferedReader in = null;
		try {
			// String topConcepts =
			// poolPropertyProperties.get("poolparty.topconcepts").toString();
			String projectId = poolPropertyProperties
					.get("poolparty.projectId").toString();
			URL url = null;
			if (schemeFlag) {
				url = new URL(serverAddress + projectId
						+ "/topconcepts?scheme=" + topConcepts
						+ "&properties=skos:narrower&locale=" + locale);
			} else {
				url = new URL(serverAddress + projectId + "/concepts?concepts="
						+ topConcepts + "&properties=skos:narrower&locale="
						+ locale);
			}
			// LOG.info(url);

			// http://tc.poolparty.biz/api/thesaurus/1DBC909E-6D1C-0001-917C-194CD8B0B2B0/concepts?concepts=http://tc.poolparty.biz/labIPTC/697&properties=skos:narrower&locale
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			String userpassword = userId + ":" + password;
			String encoded = "Basic "
					+ DatatypeConverter.printBase64Binary(userpassword
							.getBytes());
			connection.setRequestProperty("Authorization", encoded);
			connection.setDoOutput(doOutput);
			connection.setRequestProperty("Content-Type", contentType);
			connection.setConnectTimeout(connectTimeout);
			connection.setReadTimeout(readTimeout);

			// String encodedQuery = URLEncoder.encode(query.toString(),
			// "UTF-8");
			// connection.setRequestProperty("content-type",
			// encodedContentType);
			// OutputStreamWriter out = new OutputStreamWriter(
			// connection.getOutputStream());
			// out.write("query=" + encodedQuery);
			// out.write("&format="
			// + URLEncoder.encode("application/json", "utf-8"));
			// out.flush();
			// out.close();
			in = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			String line;
			content = new StringBuilder();

			// read from the urlconnection via the bufferedreader
			while ((line = in.readLine()) != null) {
				content.append(line + "\n");
			}
			LOG.debug("\nREST Service Invoked Successfully.."
					+ content.toString());
		} catch (MalformedURLException malformedURLException) {
			LOG.error(malformedURLException);
		} catch (UnsupportedEncodingException unsupportedEncodingException) {
			LOG.error(unsupportedEncodingException);
		} catch (IOException ioException) {
			LOG.error(ioException);
		} finally {
			try {
				if (in != null) {
					in.close();
				}

			} catch (IOException e) {
				LOG.error(e);
			}
		}
		if (content != null) {
			return content.toString();
		} else {
			return "";
		}

	}

	@Override
	public List<String> crawlText(String text) {
		return crawlText(text,
				poolPropertyProperties.getProperty("poolparty.language"));
	}

	@Override
	public List<String> crawlText(String text, String language) {
		String charSet = "UTF-8";
		String urlStr = poolPropertyProperties
				.getProperty("poolparty.crawlerurl");
		String user = poolPropertyProperties.getProperty("poolparty.userId");
		String password = poolPropertyProperties
				.getProperty("poolparty.password");
		String projectId = poolPropertyProperties
				.getProperty("poolparty.projectId");

		LOG.info(urlStr + "--" + user + "--" + projectId + "--" + language);
		try {

			StringBuffer query = new StringBuffer(String.format("text=%s",
					URLEncoder.encode(text, charSet)));
			query.append("&").append(
					String.format("projectId=%s",
							URLEncoder.encode(projectId, charSet)));

			query.append("&").append(
					String.format("language=%s",
							URLEncoder.encode(language, charSet)));
			URL url = new URL(urlStr);
			HttpURLConnection connection = (HttpURLConnection) url
					.openConnection();
			String userpassword = user + ":" + password;
			String encoded = "Basic "
					+ DatatypeConverter.printBase64Binary(userpassword
							.getBytes());
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Authorization", encoded);
			connection.setDoOutput(true);
			connection.setRequestProperty("Content-Type", "application/json");
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			connection.setRequestProperty("content-type",
					"application/x-www-form-urlencoded");
			connection.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(
					connection.getOutputStream());
			wr.writeBytes(query.toString());
			wr.flush();
			wr.close();

			BufferedReader in = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));

			String line;
			StringBuilder content = new StringBuilder();

			// read from the urlconnection via the bufferedreader
			while ((line = in.readLine()) != null) {
				content.append(line + "\n");
			}
			LOG.info(content.toString());

			in.close();

			List<String> tags = getTagsFromJson(content.toString());
			return tags;

		} catch (Exception e) {
			LOG.error("Error while crawling", e);
		}

		return null;
	}

	private List<String> getTagsFromJson(String content) {
		List<String> tags = new ArrayList<String>();
		JSONObject root = new JSONObject(content);
		String organizationName = poolPropertyProperties
				.getProperty("poolparty.organizationName");
		try {
			JSONArray jsonArray = root.getJSONArray("categories");
			if (jsonArray != null) {
				for (int i = 0; i < jsonArray.length(); i++) {
					JSONObject obj = jsonArray.getJSONObject(i);
					String tag = organizationName + ":"
							+ obj.getString("prefLabel");
					tags.add(tag);
				}
			}

			// check for each tag return are there any child tags under
			// categoryConceptResults
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject obj = jsonArray.getJSONObject(i);
				String parentTagName = obj.getString("prefLabel");
				StringBuffer sb = new StringBuffer();
				sb.append(organizationName).append(":");
				try {
					JSONArray subTags = obj
							.getJSONArray("categoryConceptResults");
					for (int j = 0; j < subTags.length(); j++) {
						JSONObject childTag = subTags.getJSONObject(j);
						String childTagName = childTag.getString("prefLabel");
						if (!StringUtils.equalsIgnoreCase(parentTagName,
								childTagName)) {
							sb.append(parentTagName).append("/")
									.append(childTagName);
						}
					}
				} catch (JSONException e) {
					LOG.info("No sub tags found under categoryConceptResults");
				}
				String childTagFull = sb.toString();
				if (!StringUtils.isEmpty(childTagFull)
						&& !StringUtils.equalsIgnoreCase(childTagFull,
								organizationName + ":")) {
					tags.add(childTagFull);
					LOG.info("childTags=" + childTagFull);
				}
			}

		} catch (JSONException e) {
			LOG.info("Did not get any categories [tags]");
		}

		return tags;
	}

}
