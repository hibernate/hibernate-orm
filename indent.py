import re
import sys
sys.argv.pop(0)
for fname in sys.argv:
	file = open(fname)
	inlines = file.read().split('\n')
	file.close()
	out= []
	tabcount = 0
	extratab=0
	for ln in inlines:
		code = ln.lstrip('\t ').rstrip('\t ')
		clen = len(code)
		javadoc = clen > 0 and code[0]=='*'
		if javadoc:
			code = ' ' + code
		else:
			begincb = clen > 0 and ( code[0]=='}' or code[0]==')' )
			tabcount -= begincb
		
		extratab = extratab or ( len(code)>0 and code[0]=='.' )
		
		tabs = '\t' * (tabcount + extratab)
		
		extratab = clen>0 and ( code[clen-1]==':' or code[clen-1]=='?' )
		
		if clen>5 and code[0:6] == '} else':
			code = '}\n' + tabs + 'else' + code[6:]
		if clen>6 and code[0:7] == '} catch':
			code = '}\n' + tabs + 'catch' + code[7:]
		
		out.append( tabs + code + '\n' )
		
		if not javadoc:
			uncommented = code.split('//')[0].rstrip()
			clen = len(uncommented)
			endob = clen > 0 and ( code[clen-1]=='{' or code[clen-1]=='(' )
			tabcount += endob
		
	file = open( fname, 'w' )
	for ln in out:
		file.write(ln)
	file.close()


