package org.apache.zookeeper.server.browser;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.apache.velocity.runtime.parser.ParseException;
import org.apache.velocity.runtime.parser.node.SimpleNode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.DataTree;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;


public class FileBrowserServlet extends HttpServlet {

	static final Logger logger = LoggerFactory.getLogger(FileBrowserServlet.class);


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	ZooKeeperServer zkServer;

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html");

		String mainTemplate = "error";

		String layout = "";

		boolean shouldParse = true;

		Stat stat = new Stat();

		String baseNode = request.getRequestURL().toString().replaceAll(".*/commands/browser/?", "/");

		Template template = null;

		Template layoutTemplate = null;

		DataTree tree = zkServer.getZKDatabase().getDataTree();

		if ("".equals(baseNode)) {
			baseNode = "";
		}

		//mainTemplate = new String(zkServer.getZKDatabase().getData("/configs/browser/views/application" + baseNode, stat, null));
		template = getTemplate("main", "/configs/browser/views/application/index.html");
		layoutTemplate = getTemplate("layout", "/configs/browser/views/layouts/application.html");

		if (template == null) {
			logger.error("loading " +  "/configs/browser/assets" + baseNode);
			template = getTemplate("x", "/configs/browser/assets" + baseNode);
			response.getWriter().write("loading " + "/configs/browser/assets" + baseNode);
			return;
		}

		boolean isFolder = false;

		// check if exists a folder
		if (!baseNode.endsWith("/")) {

			try {
				List<String> nodes = tree.getChildren(baseNode+"/", stat, null);

				isFolder = (nodes != null);
				
			    response.getWriter().write("isFolder " + isFolder);

			} catch (NoNodeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}



		StringWriter sw = new StringWriter();

		VelocityContext ctx = new VelocityContext();

		try {
			List<String> nodes = null;


			if (isFolder) {
				nodes = tree.getChildren(baseNode+"/", stat, null);

			} else {
				nodes = tree.getChildren(baseNode, stat, null);
			}

			ctx.put("nodes", nodes);


		} catch (NoNodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}




		byte[] b;
		try {
			b = tree.getData(baseNode, stat, null);

			if (b != null) {

				String data = new String(b);

				ctx.put("data", escapeHtml(data));

			}
		} catch (NoNodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}






		ctx.put("path", baseNode);

		template.merge(ctx, sw);

		VelocityContext layoutCtx = new VelocityContext();

		layoutCtx.put("screen_content", sw.toString());

		sw.close();
		sw = new StringWriter();

		layoutTemplate.merge(layoutCtx, sw);

		response.getWriter().write(sw.toString());

	}

	public void setZkServer(ZooKeeperServer zkServer) {
		this.zkServer = zkServer;
	}

	/*	private StringBuilder getTemplate(ContentStreamBase content) {
		InputStream stream = null;

		StringBuilder sb = new StringBuilder();

		BufferedReader br;

		String line = "";

		try {
			stream = content.getStream();

			br = new BufferedReader(new InputStreamReader(stream));
			while ((line = br.readLine()) != null) {
				if (line.matches(".*src=\"\\/.*")) {
					//TODO solr/dbpedia OFCOURSE must be parametric / automatic (better!) 
					line = line.replaceAll("src=\"", "src=\"/solr/dbpedia_shard1_replica1/admin/file?file=");
				} 
				sb.append(line);

			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.error("error on filtering ", e);
		} finally {
			IOUtils.closeQuietly(stream);
		}

		return sb;
	}*/


	private Template getTemplate(String templateName, String path) {

		String source = null;

		Template template = null;

		try {
			Stat stat = new Stat();
			source = new String(zkServer.getZKDatabase().getData(path, stat, null));
		} catch (NoNodeException e) {
			// TODO Auto-generated catch block
			return null;

		}

		try
		{
			//TODO what if fname is null?
			template = Velocity.getTemplate(templateName);
		}
		catch( ResourceNotFoundException rnfe )
		{
			// couldn't find the template, try to load it

			// TODO it should be fired only for SOME mimetypes (..through an annotation??)


			RuntimeServices runtimeServices = RuntimeSingleton.getRuntimeServices();
			StringReader reader = new StringReader(source);

			SimpleNode node = null;

			try {
				node = runtimeServices.parse(reader, templateName);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				logger.error("error while parsing new template", e);
			}

			template = new Template();

			template.setRuntimeServices(runtimeServices);

			if (node != null) {
				template.setData(node);
			} else {
				logger.error("node null, can't set on template");
			}


			template.initDocument();

		}
		catch( ParseErrorException pee )
		{
			// syntax error: problem parsing the template
			logger.error("error while parsing template: ", pee);

		}
		catch( MethodInvocationException mie )
		{
			// something invoked in the template
			// threw an exception
			logger.error("error while parsing temaplate: ", mie);
		}
		catch( Exception e ) { 
			logger.error("error while parsing temaplate: ", e);
		}

		return template;
	}

}

