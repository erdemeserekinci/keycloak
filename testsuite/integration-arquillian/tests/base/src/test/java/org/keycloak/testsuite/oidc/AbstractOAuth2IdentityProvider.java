package org.keycloak.testsuite.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.authorization.ResourceServerRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

@AuthServerContainerExclude(AuthServer.REMOTE)
public class AbstractOAuth2IdentityProvider extends AbstractKeycloakTest {

	@Override
	public void addTestRealms(List<RealmRepresentation> testRealms) {
		testRealms.add(RealmBuilder.create().name(TEST)
				.client(ClientBuilder.create().clientId("myclient")
						.secret("secret")
						.authorizationServicesEnabled(true)
						.redirectUris("http://localhost/myclient")
						.defaultRoles(
								"client-role-1",
								"client-role-2",
								"Acme administrator",
								"Acme viewer",
								"tenant administrator",
								"tenant viewer",
								"tenant user"
						)
						.build())
				.build());
	}

	public static void setupAbstractOAuth2IdentityProvider(KeycloakSession session) throws IOException {
		RealmModel realm = session.realms().getRealmByName(TEST);
		session.getContext().setRealm(realm);

		TestProvider tested = getTested(session);

		JsonNode jsonNode = tested
				.asJsonNode("{\"nullone\":null, \"emptyone\":\"\", \"blankone\": \" \", \"withvalue\" : \"my value\", \"withbooleanvalue\" : true, \"withnumbervalue\" : 10}");
		Assert.assertNull(tested.getJsonProperty(jsonNode, "nonexisting"));
		Assert.assertNull(tested.getJsonProperty(jsonNode, "nullone"));
		Assert.assertNull(tested.getJsonProperty(jsonNode, "emptyone"));
		Assert.assertEquals(" ", tested.getJsonProperty(jsonNode, "blankone"));
		Assert.assertEquals("my value", tested.getJsonProperty(jsonNode, "withvalue"));
		Assert.assertEquals("true", tested.getJsonProperty(jsonNode, "withbooleanvalue"));
		Assert.assertEquals("10", tested.getJsonProperty(jsonNode, "withnumbervalue"));
		assertEquals("Response status is not as expected!", tested.getClientRealm(), TEST);
	}

	@Test
	public void abstractOAuth2IdentityProvider() throws IOException {
		ClientsResource clients = getAdminClient().realms().realm(TEST).clients();
		ClientRepresentation client = clients.findByClientId("myclient").get(0);
		ResourceServerRepresentation settings = JsonSerialization.readValue(getClass().getResourceAsStream("/authorization-test/acme-resource-server-cleanup-test.json"), ResourceServerRepresentation.class);

		clients.get(client.getId()).authorization().importSettings(settings);

		testingClient.server().run(AbstractOAuth2IdentityProvider::setupAbstractOAuth2IdentityProvider);
	}

	private static class TestProvider extends org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider<OAuth2IdentityProviderConfig> {

		public TestProvider(OAuth2IdentityProviderConfig config, KeycloakSession session) {
			super(session, config);
		}

		@Override
		protected String getDefaultScopes() {
			return "default";
		}

		protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
			return new BrokeredIdentityContext(accessToken);
		};

	};

	private static TestProvider getTested(KeycloakSession session) {
		return new TestProvider(getConfig(null, null, null, Boolean.FALSE), session);
	}

	private static OAuth2IdentityProviderConfig getConfig(final String autorizationUrl, final String defaultScope,
			final String clientId, final Boolean isLoginHint) {
		IdentityProviderModel model = new IdentityProviderModel();
		OAuth2IdentityProviderConfig config = new OAuth2IdentityProviderConfig(model);
		config.setAuthorizationUrl(autorizationUrl);
		config.setDefaultScope(defaultScope);
		config.setClientId(clientId);
		config.setLoginHint(isLoginHint);
		return config;
	}

}

