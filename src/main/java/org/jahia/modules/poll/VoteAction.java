/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.poll;

import org.jahia.bin.Action;
import org.jahia.services.content.JCRSessionWrapper;
import org.slf4j.Logger;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.bin.ActionResult;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.URLResolver;
import org.json.JSONObject;

import javax.jcr.NodeIterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;


/**
 * User: fabrice
 * Date: Apr 15, 2010
 * Time: 10:30:20 AM
 */
public class VoteAction extends Action {
    private transient static Logger logger = org.slf4j.LoggerFactory.getLogger(VoteAction.class);

    // Vote action
    public ActionResult doExecute(HttpServletRequest req, RenderContext renderContext, Resource resource,
                                  JCRSessionWrapper session, Map<String, List<String>> parameters, URLResolver urlResolver) throws Exception {

        String answerUUID = req.getParameter("answerUUID");

        // Poll Node management
        // Get the pollNode
        JCRNodeWrapper pollNode = session.getNodeByUUID(renderContext.getMainResource().getNode().getIdentifier());

        // Update total of votes (poll node)
        long totalOfVotes = pollNode.getProperty("totalOfVotes").getLong();
        pollNode.setProperty("totalOfVotes", totalOfVotes + 1);

        // Answer node management
        // Get the answer node
        JCRNodeWrapper answerNode = session.getNodeByUUID(answerUUID);

        // Increment nb votes
        long nbOfVotes = answerNode.getProperty("nbOfVotes").getLong();
        answerNode.setProperty("nbOfVotes", nbOfVotes + 1);

        // Save
        session.save();

        return new ActionResult(HttpServletResponse.SC_OK, null, generateJSONObject(pollNode));
    }

    public static JSONObject generateJSONObject(JCRNodeWrapper pollNode) throws Exception {
        NodeIterator answerNodes = pollNode.getNode("answers").getNodes();

        Map<String, Object> props = new HashMap<String, Object>();
        ArrayList<Map<String, Object>> answersContainer = new ArrayList<Map<String, Object>>();

        long totalVote = pollNode.getProperty("totalOfVotes").getLong();

        // Poll name + total votes
        props.put("totalOfVotes", totalVote);
        props.put("question", pollNode.getProperty("question").getString());

        // Each answer name + total vote
        while (answerNodes.hasNext()) {
            Map<String, Object> answerProperties = new HashMap<String, Object>();
            javax.jcr.Node answer = answerNodes.nextNode();
            long answerVotes = answer.getProperty("nbOfVotes").getLong();

            answerProperties.put("label", answer.getProperty("label").getString());
            answerProperties.put("nbOfVotes", answerVotes);
            answerProperties.put("percentage", (totalVote == 0 || answerVotes == 0) ? 0 : (answerVotes / totalVote) * 100);

            answersContainer.add(answerProperties);
        }

        props.put("answerNodes", answersContainer.toArray());

        return new JSONObject(props);
    }
}
