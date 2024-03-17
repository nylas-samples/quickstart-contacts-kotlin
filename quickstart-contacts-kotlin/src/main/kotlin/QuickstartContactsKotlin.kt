import com.nylas.NylasClient
import com.nylas.models.*
import io.github.cdimascio.dotenv.dotenv
import spark.kotlin.Http
import spark.kotlin.ignite

fun main(args: Array<String>) {
    val dotenv = dotenv()

    val nylas = NylasClient(
        apiKey = dotenv["NYLAS_API_KEY"],
        apiUri = dotenv["NYLAS_API_URI"]
    )

    val http: Http = ignite()

    http.get("/nylas/auth") {
        val scope = listOf("https://www.googleapis.com/auth/calendar", "https://www.googleapis.com/auth/calendar.events")
        val config : UrlForAuthenticationConfig = UrlForAuthenticationConfig(
            dotenv["NYLAS_CLIENT_ID"],
            "http://localhost:4567/oauth/exchange",
            AccessType.ONLINE,
            AuthProvider.GOOGLE,
            Prompt.DETECT,
            scope,
            true,
            "sQ6vFQN",
            "atejada@gmail.com")

        val url = nylas.auth().urlForOAuth2(config)
        response.redirect(url)
    }

    http.get("/oauth/exchange") {
        val code : String = request.queryParams("code")
        if(code == "") { response.status(401) }
        val codeRequest : CodeExchangeRequest = CodeExchangeRequest(
            "http://localhost:4567/oauth/exchange",
            code,
            dotenv["NYLAS_CLIENT_ID"],
            null,
            null
        )
        try {
            val codeResponse : CodeExchangeResponse = nylas.auth().exchangeCodeForToken(codeRequest)
            request.session().attribute("grant_id",codeResponse.grantId)
            codeResponse.grantId
        }catch (e : Exception){
            e.toString()
        }
    }

    http.get("/nylas/list-contacts") {
        try{
            val listContactsQueryParams = ListContactsQueryParams.Builder().limit(5).build()
            val contacts = nylas.contacts().list(request.session().attribute("grant_id"),
                listContactsQueryParams).data
            contacts
        }catch (e : Exception){
            e.toString()
        }
    }

    http.get("/nylas/create-contact") {
        try {
            val emails : List<ContactEmail> = listOf(ContactEmail("swag@nylas.com", "work"))
            val webpage : List<WebPage> = listOf(WebPage("https://www.nylas.com", "work"))

            val contactRequest = CreateContactRequest.Builder().
            emails(emails).
            companyName("Nylas").
            givenName("Nylas' Swag").
            notes("This is good swag").
            webPages(webpage).
            build()

            val contact = nylas.contacts().create(request.session().attribute("grant_id"), contactRequest)
            contact
        }catch (e : Exception){
            e.toString()
        }
    }
}