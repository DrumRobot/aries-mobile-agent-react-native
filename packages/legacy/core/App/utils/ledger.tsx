import TcpSocket from 'react-native-tcp-socket'

import pools from '../../configs/ledgers/indy'
import { GenesisTransaction } from '../types/genesis'

export const canConnectToLedgerNode = async (node: { host: string; port: number }): Promise<boolean> =>
  new Promise((resolve) => {
    const socketTimeoutInMs = 3000
    const client = TcpSocket.createConnection(node, () => {
      resolve(true)
      client.destroy()
    })

    // Other events that can be safely be ignored. See the
    // library for more details:
    // https://www.npmjs.com/package/react-native-tcp-socket

    client.on('error', (e: Error) => {
      console.error(e)
      client.destroy()
      resolve(false)
    })

    client.on('timeout', () => {
      console.error(`${node.host}:${node.port} timeout`)
      client.destroy()
      client.removeAllListeners()

      resolve(false)
    })

    client.setTimeout(socketTimeoutInMs)
  })

export const fetchLedgerNodes = (indyNamespace = 'sovrin'): Array<{ host: string; port: number }> => {
  const [pool] = pools.filter((p) => p.indyNamespace === indyNamespace)
  if (!pool) {
    return []
  }

  const genesisTransactionsAsString = pool.genesisTransactions
  let genesisTransactions: Array<GenesisTransaction> = []

  if (genesisTransactionsAsString) {
    try {
      genesisTransactions = genesisTransactionsAsString.split('\n').map((g) => JSON.parse(g))
    } catch (error: unknown) {
      return []
    }
  }

  const nodes = genesisTransactions.map((g) => {
    return { host: g.txn.data.data.client_ip, port: parseInt(g.txn.data.data.client_port) }
  })

  return nodes
}
