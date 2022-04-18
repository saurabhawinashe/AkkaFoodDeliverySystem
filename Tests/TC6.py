# Checks if the agent is Signed out and also after signing 
# out, the status of agent must be signed-out


from http import HTTPStatus
import requests


# RESTAURANT SERVICE    : http://localhost:8080
# DELIVERY SERVICE      : http://localhost:8081
# WALLET SERVICE        : http://localhost:8082

def test():
	test_result = 'Pass'

    # Reinitialize Delivery service
	http_response = requests.post("http://localhost:8081/reInitialize")

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail'

	# Agent 201 sign in
	http_response = requests.post("http://localhost:8081/agentSignIn", json={"agentId": 201})

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail'

	# Agent 202 sign in
	http_response = requests.post("http://localhost:8081/agentSignIn", json={"agentId": 202})

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail'

	# Agent 203 sign in
	http_response = requests.post("http://localhost:8081/agentSignIn", json={"agentId": 203})

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail'

	# Check Agent 201 status
	http_response = requests.get("http://localhost:8081/agent/201")

	if(http_response.status_code != HTTPStatus.OK):
		test_result = 'Fail'

	res_body = http_response.json()

	agent_id = res_body.get("agentId")
	status = res_body.get("status")

	if agent_id != 201 or status != "available":
		test_result = 'Fail'

	# Check Agent 202 status
	http_response = requests.get("http://localhost:8081/agent/202")

	if(http_response.status_code != HTTPStatus.OK):
		test_result = 'Fail'
	
	res_body = http_response.json()

	agent_id = res_body.get("agentId")
	status = res_body.get("status")
	
	if agent_id != 202 or status != "available":
		test_result = 'Fail'

	# Check Agent 203 status
	http_response = requests.get("http://localhost:8081/agent/203")

	if(http_response.status_code != HTTPStatus.OK):
		test_result = 'Fail'
	
	res_body = http_response.json()

	agent_id = res_body.get("agentId")
	status = res_body.get("status")
	
	if agent_id != 203 or status != "available":
		test_result = 'Fail'


	# Agent 201 sign out
	http_response = requests.post("http://localhost:8081/agentSignOut", json={"agentId": 201})

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail'

	# Agent 202 sign out
	http_response = requests.post("http://localhost:8081/agentSignOut", json={"agentId": 202})

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail'

	# Agent 203 sign out
	http_response = requests.post("http://localhost:8081/agentSignOut", json={"agentId": 203})

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail'

	# Check Agent 201 status
	http_response = requests.get("http://localhost:8081/agent/201")

	if(http_response.status_code != HTTPStatus.OK):
		test_result = 'Fail'

	res_body = http_response.json()

	agent_id = res_body.get("agentId")
	status = res_body.get("status")

	if agent_id != 201 or status != "signed-out":
		test_result = 'Fail'

	# Check Agent 202 status
	http_response = requests.get("http://localhost:8081/agent/202")

	if(http_response.status_code != HTTPStatus.OK):
		test_result = 'Fail'
	
	res_body = http_response.json()

	agent_id = res_body.get("agentId")
	status = res_body.get("status")
	
	if agent_id != 202 or status != "signed-out":
		test_result = 'Fail'

	# Check Agent 203 status
	http_response = requests.get("http://localhost:8081/agent/203")

	if(http_response.status_code != HTTPStatus.OK):
		test_result = 'Fail'
	
	res_body = http_response.json()

	agent_id = res_body.get("agentId")
	status = res_body.get("status")
	
	if agent_id != 203 or status != "signed-out":
		test_result = 'Fail'
	

	return test_result


if __name__ == "__main__":
	test_result = test()
	print(test_result)
