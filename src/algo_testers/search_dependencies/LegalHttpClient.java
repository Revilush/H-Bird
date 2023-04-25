package algo_testers.search_dependencies;

public class LegalHttpClient extends BaseHttpClient {

	public LegalHttpClient(boolean closeConnectionAfterRequest) {
		super(closeConnectionAfterRequest);
	}

	public LegalHttpClient(boolean closeConnectionAfterRequest, boolean compressionEnabled) {
		super(closeConnectionAfterRequest, compressionEnabled);
	}

	
	
}
