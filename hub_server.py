#!python3

#Imports
import os
import sys
import time
import logging
from importlib import import_module
from subprocess import Popen


#Hub LIB
from hub_lib import *



#Module commands
commands = {}
command_restrict = {}
command_args = {}
command_desc = {}


### INCOMING REQUESTS ###
listener = Flask(__name__)
@listener.route('/', methods=["POST"])
def cmd_post():
	if request.method == "POST":

		#Get Variables
		command = str(request.form["command"]) if "command" in request.form else ""
		auth_key = str(request.form["auth"]) if "auth" in request.form else ""
		ip = str(request.remote_addr)
		status_code = 200
		
		#Command received
		#log_msg(str(command), "from", ip, "auth '" + str(auth) + "' level", str(level), display=False)


		#Check authorization
		auth = authorize(auth_key)

		if auth[0]:
			result = cmd_handle(command, auth_key, auth[1], ip)
		else:
			result = AUTH_FAIL
			status_code = 401
		
		#Send response
		resp = Response(str(result), mimetype="text/xml")
		resp.headers["Access-Control-Allow-Origin"] = "*"
		return resp, status_code



### COMMAND HANDLING ###
def get_cmd_restriction(cmd):
	if cmd in command_restrict:
		return command_restrict[cmd]
	else:
		return 0

def cmd_handle(cmdargs, auth, level, ip):

	#Separate arguments from command
	sp = cmdargs.split(" ")
	command = sp[0].lower()
	args = sp[1:]

	#Command received
	if get_cmd_restriction(command) >= 0:
		log_msg(CMD_RECV, "'" + str(cmdargs) + "'", "from", ip, "auth '" + str(auth) + "' level", str(level), header=TITLE_SERVER, log=SERVER_LOG)

	#Check if command exists in any module
	if command in commands:

		#Authorization level
		if level >= get_cmd_restriction(command):
			#Run command in module
			try:
				result = commands[command](*args)
			except Exception as e:
				result = (0, command + " encountered an error while running: " + repr(e))
		else:
			result = (0, "Authorization level too low to run this command.")
			
		#Print result
		if get_cmd_restriction(command) >= 0:
			log_msg(result[1])

		return result[1]
	else: #Command not found
		if get_cmd_restriction(command) >= 0:
			log_msg(CMD, command, CMD_NOTFOUND, header=TITLE_SERVER, log=SERVER_LOG)
		return CMD + " " + command + " " + CMD_NOTFOUND



### MODULES ###
def load_modules():
	#Add reload hub command
	commands["reloadhub"] = reload_hub
	command_args["reloadhub"] = ""
	command_desc["reloadhub"] = "Restart HUB server process. Hardware will remain online. Could take up to 30s."

	#Add help command
	commands["help"] = cmd_help
	command_args["help"] = ""
	command_desc["help"] = "Shows this menu. Help <command> for details on a specific command."

	#Load modules and their commands
	path = os.path.dirname(os.path.realpath(__file__)) + "/modules"
	for file in os.listdir(path):
		if file[-3:]==".py" and not "__init__" in file and not "default_module" in file:
			file = file.replace(".py","")

			try:
				module = getattr(import_module("modules." + file), file)()

				#Get interface functions
				modcomms = module.get_commands()

				#Call module init
				log_msg(MODULE_OK, MODULE, file, MODULE_INIT, header=TITLE_SERVER)

				#Load module commands
				for c,f in modcomms.items():
					cmd = c.lower().split(" ")[0]
					if not cmd in commands: #New command	
						#Register commands in dictionaries
						commands[cmd] = f
						args = module.get_args(cmd)
						desc = module.get_desc(cmd)
						restriction = module.get_restriction(cmd)

						command_restrict[cmd] = restriction
						if args != "": command_args[cmd] = args
						if desc != "": command_desc[cmd] = desc

						log_msg(" "*len(MODULE_OK), CMD_ADD, CMD.lower(), cmd)
					else: #Command already exists
						log_msg(" "*len(MODULE_OK), ERROR, CMD_EXISTS, cmd)
			except Exception as e:
				log_msg("Error loading module " + file + ":" + repr(e), log=SERVER_LOG, header=TITLE_SERVER)
				return BOOT_FAILURE

	#Successful load
	return BOOT_SUCCESS

### BUILT IN COMMANDS ###
def reload_hub():
	request.environ.get('werkzeug.server.shutdown')() #Shutdown flask listener
	exec_cmd(time=5, func=Popen, args=[[PYTHON_CMD, os.path.abspath(__file__)]])
	return (1,"Reloading HUB process")


def cmd_help(*args):
	if len(args) > 0:
		cmds = [a for a in args if a != ""]
	else:
		cmds = commands.keys()

	if len(cmds)>0:
		s = "Supported commands\n"
	
		for cmd in cmds:
			if cmd in commands:
				s += (" "*len(MODULE_OK) + cmd + "(" + CMD_LVL + " " + str(command_restrict[cmd] if cmd in command_restrict else 0) + "): ")
				if cmd in command_args:
					s += command_args[cmd]
					s += "\n"
				if cmd in command_desc: 
					s += (" "*len(MODULE_OK)*2 + command_desc[cmd] + "\n")
					s +=  "\n"

		if len([c for c in commands.keys() if c in cmds]) > 0:
			return (1,s)
		else:
			return (0,"Commands ("+",".join(cmds)+") not found")


	

### MAIN ###
def main():
	log_msg(LOADING, header=TITLE_SERVER)


	#Load modules
	log_msg(load_modules(), header=TITLE_SERVER)
	

	#Set FLASK logging to verbose (only error)
	log = logging.getLogger('werkzeug')
	log.setLevel(logging.ERROR)

	#Start FLASK listening on PORT
	#listener.debug = True
	listener.run(host="0.0.0.0", port=PORT)


if __name__ == "__main__":
	main()