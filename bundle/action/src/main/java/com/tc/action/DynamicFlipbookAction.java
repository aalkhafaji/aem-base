/**
 * 
 */
package com.tc.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tc.model.FlipbookBean;

/**
 * The Class DynamicFlipbookAction.
 *
 * @author gdinakar
 */
public class DynamicFlipbookAction extends BaseAction {
	
	/** The Constant LOG. */
	private static final Logger LOG = LoggerFactory.getLogger(DynamicFlipbookAction.class);
	
	/**
	 * Gets the images paths.
	 *
	 * @return the images paths
	 * @throws RepositoryException the repository exception
	 */
	public FlipbookBean getImagesPaths() throws RepositoryException {
		LOG.info("Entered getImagesPaths method of DynamicFlipbookAction");
		
		FlipbookBean flipbookBean = null;
		List<String> imagesPathList = null;
		String currentPageURL = getSlingRequest().getRequestURL().toString();
		Node currentNode = getCurrentNode();
		String pdfPath = null;
		if(currentNode != null) {
			
			int lastIndexOfSlash = currentPageURL.lastIndexOf("/");
			String tempURL = currentPageURL.substring((lastIndexOfSlash+1), currentPageURL.length()).replace(".html", "");
			tempURL = tempURL.replaceAll("\\.", "/");
			tempURL = tempURL.substring(tempURL.indexOf("/"), tempURL.length());
			tempURL = tempURL.replace("/pdf", ".pdf");
			pdfPath = "/content"+tempURL;
			if(pdfPath.contains("%20")) {
				pdfPath = pdfPath.replaceAll("%20", " ");
			} 
			Node pdfNode = getSlingRequest().getResourceResolver().getResource(pdfPath).adaptTo(Node.class);
			String pdfName = pdfNode.getName().replace(".pdf", "");
			final String pdfImagePath = pdfPath+"/jcr:content/renditions/"+pdfName+"_image";
			if(pdfNode.hasNode("jcr:content") && pdfNode.getNode("jcr:content").hasNode("renditions")) {
				Node renditionsNode = pdfNode.getNode("jcr:content").getNode("renditions");
				if(renditionsNode.hasNodes()) {
					NodeIterator imageNodes = renditionsNode.getNodes();
					Node imageNode = null;
					String imageName = null;
					String imagePath = null;
					flipbookBean = new FlipbookBean();
					imagesPathList = new ArrayList<String>();
					while(imageNodes.hasNext()) {
						imageNode = imageNodes.nextNode();
						imageName = imageNode.getName();
						if(imageName.endsWith(".jpg")) {
							imagePath = imageNode.getPath();
							imagesPathList.add(imagePath);
						}
					}
					// Sorting images based on image number
					Collections.sort(imagesPathList, new Comparator<String>() {

						@Override
						public int compare(String imagePath1, String imagePath2) {
							String tempImagePath1 = imagePath1.replaceAll(pdfImagePath, "");
							String finalImagePath1 = tempImagePath1.replaceAll(".jpg", "");
							String tempImagePath2 = imagePath2.replaceAll(pdfImagePath, "");
							String finalImagePath2 = tempImagePath2.replaceAll(".jpg", "");
							return new Integer(finalImagePath1).compareTo(new Integer(finalImagePath2));
						}
						
					});
					flipbookBean.setImagesPathList(imagesPathList);
				}
			} else {
				LOG.info(pdfNode+" node might not contain /jcr:content/renditions node");
			}
		}
		return flipbookBean;
	}
}
