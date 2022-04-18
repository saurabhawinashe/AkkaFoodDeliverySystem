from http import HTTPStatus
import requests
import time

# Tries to place an order when all the agents are already assigned to different orders
# This order should be in unassigned state and no agent should be assigned to it


# RESTAURANT SERVICE    : http://localhost:8080
# DELIVERY SERVICE      : http://localhost:8081
# WALLET SERVICE        : http://localhost:8082


def test():

	test_result = 'Pass'

	# Reinitialize Restaurant service
	http_response = requests.post("http://localhost:8080/reInitialize")

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail'

	# Reinitialize Delivery service
	http_response = requests.post("http://localhost:8081/reInitialize")

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail'

	# Reinitialize Wallet service
	http_response = requests.post("http://localhost:8082/reInitialize")

	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = 'Fail'



	#Sign in agent 201
	http_response = requests.post("http://localhost:8081/agentSignIn", json={"agentId":201})
	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = "Fail1"

	#Sign in agent 202
	http_response = requests.post("http://localhost:8081/agentSignIn", json={"agentId":202})
	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = "Fail2"


	#Sign in agent 203
	http_response = requests.post("http://localhost:8081/agentSignIn", json={"agentId":203})
	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = "Fail3"

	time.sleep(4)


	#Try to place a valid order
	http_response = requests.post("http://localhost:8081/requestOrder", json={"custId":301, "restId":101, "itemId":1, "qty":2})

	res_body = http_response.json()
	order_id = -1
	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = "Fail4"
	else:
		order_id = res_body.get("orderId")

	if (order_id == -1):
		test_result = 'Fail5'

	time.sleep(2)

	#Check the order status. It should be assigned to agent 201
	http_response = requests.get("http://localhost:8081/order/"+str(order_id))

	res_body = http_response.json()
	if(http_response.status_code != HTTPStatus.OK):
		test_result = "Fail6"
	else:
		agent_id = res_body.get("agentId")
		status = res_body.get("status")

		if(status != "assigned" or agent_id != 201):
			print(status + str(agent_id))
			test_result = "Fail7"
	

	



	#Try to place a valid order
	http_response = requests.post("http://localhost:8081/requestOrder", json={"custId":302, "restId":101, "itemId":1, "qty":2})

	res_body = http_response.json()
	order_id = -1
	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = "Fail"
	else:
		order_id = res_body.get("orderId")

	if (order_id == -1):
		test_result = 'Fail'


	time.sleep(1)
	#Check the order status. It should be assigned to agent 202
	http_response = requests.get("http://localhost:8081/order/"+str(order_id))

	res_body = http_response.json()
	if(http_response.status_code != HTTPStatus.OK):
		test_result = "Fail"
	else:
		agent_id = res_body.get("agentId")
		status = res_body.get("status")

		if(status != "assigned" or agent_id != 202):
			test_result = "Fail"

	

	


	#Try to place a valid order
	http_response = requests.post("http://localhost:8081/requestOrder", json={"custId":301, "restId":101, "itemId":2, "qty":2})

	res_body = http_response.json()
	order_id = -1
	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = "Fail"
	else:
		order_id = res_body.get("orderId")

	if (order_id == -1):
		test_result = 'Fail'


	time.sleep(1)
	#Check the order status. It should be assigned to agent 203
	http_response = requests.get("http://localhost:8081/order/"+str(order_id))

	res_body = http_response.json()
	if(http_response.status_code != HTTPStatus.OK):
		test_result = "Fail"
	else:
		agent_id = res_body.get("agentId")
		status = res_body.get("status")

		if(status != "assigned" or agent_id != 203):
			test_result = "Fail"
	





	#Try to place a valid order
	http_response = requests.post("http://localhost:8081/requestOrder", json={"custId":302, "restId":101, "itemId":2, "qty":2})

	res_body = http_response.json()
	order_id = -1
	if(http_response.status_code != HTTPStatus.CREATED):
		test_result = "Fail"
	else:
		order_id = res_body.get("orderId")

	if (order_id == -1):
		test_result = 'Fail'

	time.sleep(1)
	#Check the order status. It should be unassigned
	http_response = requests.get("http://localhost:8081/order/"+str(order_id))

	res_body = http_response.json()
	if(http_response.status_code != HTTPStatus.OK):
		test_result = "Fail"
	else:
		agent_id = res_body.get("agentId")
		status = res_body.get("status")

		if(status != "unassigned" or agent_id != -1):
			test_result = "Fail"



	return test_result


if __name__ == "__main__":
	test_result = test()
	print(test_result)
